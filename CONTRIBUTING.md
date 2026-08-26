# How to contribute

In this document, **I** refers to project author _LooKeR_. I'm really glad you're reading this, because we need volunteer developers to help this project come to fruition.

## Forms of contributions

### Translations

Help us translate Droid-ify at [Weblate](https://hosted.weblate.org/engage/droidify)

### Bug reports

Help us improve Droid-ify experience by filing [bug reports](https://github.com/Droid-ify/client/issues/new/choose)

### Code

Code contributions are welcome and guidelines are provided in sections below

## AI Contribution

AI/LLM are still relatively new form of code generation, with questionable training ethics and practices. I am not strongly opposed to AI-generated contributions if you have interacted with the community, for example have a chat with an issue creator and project maintainer regarding the said issue, understand their expectation of a fix/implementation and then start working on it.

One of my plan is to move to [Codeberg](https://codeberg.org) which strictly bans LLM contributions, atleast on large scales due to its unclear copyright and ehtics. We already have a [codeberg organization](https://codeberg.org/droidify) but due to large number of issues in GitHub the migration is delayed.

## Submitting changes

Please send a [GitHub Pull Request to Droid-ify](https://github.com/Droid-ify/client/pull/new/main) with a clear list of what you've done. When you send a pull request, we will love you forever if you include some proof of implementation, this can be screenshots for UI implementation or screen recording for crash fixes. We can always use more test coverage. Please follow our coding conventions (below) and make sure all of your commits are atomic (one feature per commit).

Before sending a PR create a issue if it does not exist yet and discuss about it, I am a eternally offline being, please understand that I do a job and also contribute to multiple projects so I might not respond for a while.

Always write a clear log message for your commits. One-line messages are fine for small changes, but bigger changes should look like this:

```sh
git commit -m "fix: A brief summary of the commit" -m "A paragraph describing what changed and its impact"
```

## Coding conventions

We do not follow any strict coding conventions, that being said I prefer to use Data-driven style over Object-oriented because that is what computers are supposed to do, i.e. work with data. Some good rule of thumbs are provided by Gerard Holzmann in **The Power of 10 Rules**, [wiki](https://en.wikipedia.org/wiki/The_Power_of_10:_Rules_for_Developing_Safety-Critical_Code), these can help us write safer code but there is a small gap between expectation from NASA vs Droid-ify we can work with less strict guidelines which are more focused towards modern compilers (please provide me some suggestions for such minimal coding conventions).

---

Part of this document was inspired by [opengovernment CONTRIBUTING.md](https://github.com/opengovernment/opengovernment/blob/master/CONTRIBUTING.md)
