from glob import glob

locales = set()

"""
List all folders that contain translated strings
"""
locales.update(glob("core/common/src/main/res/values-*/strings.xml"))

"""
Extract folder name from paths:
    core/common/src/main/res/values-abc/strings.xml -> values-abc 
"""
locales = {path.split("/")[-2] for path in locales}

"""
Extract just the language code from folder name:
    values-abc -> abc
"""
locales = {name[len("values-"):] for name in locales}

"""
Function that updates a file with latest locales
"""
def update_file(path: str, marker_start: str, marker_end: str, line_separator: str, langs: str):
    src = open(path, 'r', encoding='utf-8').read()

    # find all text before the list and after it
    before_src, src = src.split(marker_start)
    src, after_src = src.split(marker_end)

    # figure out indent size
    indent = ""
    for line in src.split("\n"):
        curr_indent = ""
        for c in line:
            if c in [' ', '\t']:
                curr_indent += c
            else:
                break
        if len(curr_indent) > len(indent):
            indent = curr_indent

    # create the list
    src = line_separator.join([f'{indent}{lang}' for lang in sorted(langs)])

    # write to the file
    open(path, "w+", encoding='utf-8').write(
        before_src +
        marker_start +
        "\n" +
        src +
        "\n" +
        indent + marker_end +
        after_src
    )


"""
Update build.gradle.kts
"""
update_file(
    path="app/build.gradle.kts",
    marker_start="/* locale list begin */",
    marker_end="/* locale list end */",
    line_separator=",\n",
    langs=[f'"{lang}"' for lang in locales] # add quotes around each lang
)

"""
Update locales_config.xml
"""
update_file(
    path="app/src/main/res/xml/locales_config.xml",
    marker_start="<!--locale list begin-->",
    marker_end="<!--locale list end-->",
    line_separator="\n",
    langs=[f'<locale android:name="{lang}" />' for lang in locales] # map each lang to a xml tag eg: abc -> <locale android:name="abc" />
)
