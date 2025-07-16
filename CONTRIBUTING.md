# 🤝 Contributing to Droid-ify

Thank you for your interest in contributing to Droid-ify! This document provides guidelines and information for contributors.

## 🌟 Ways to Contribute

### 🐛 Bug Reports
- **Search existing issues** before creating a new one
- **Use the bug report template** when available
- **Provide detailed information**: steps to reproduce, expected vs actual behavior
- **Include system information**: Android version, device model, app version
- **Add screenshots or screen recordings** when applicable

### 💡 Feature Requests
- **Check existing feature requests** to avoid duplicates
- **Clearly describe the feature** and its expected behavior
- **Explain the use case** and why it would be valuable
- **Consider alternatives** and mention them in your request

### 🔧 Code Contributions
- **Bug fixes** - Fix reported issues or problems you've encountered
- **New features** - Implement requested features or your own ideas
- **Performance improvements** - Optimize existing code
- **Code refactoring** - Improve code quality and maintainability
- **Tests** - Add or improve test coverage

### 🌐 Translations
- **Help translate** Droid-ify into your language via [Weblate](https://hosted.weblate.org/engage/droidify/)
- **Review existing translations** for accuracy
- **Report translation issues** in our issue tracker

### 📖 Documentation
- **Improve README** and other documentation files
- **Add code comments** for complex logic
- **Create guides** for specific features or use cases
- **Fix typos** and grammatical errors

## 🚀 Getting Started

### 1. Fork and Clone
```bash
# Fork the repository on GitHub, then clone your fork
git clone https://github.com/your-username/Droid-ify.git
cd Droid-ify
```

### 2. Set Up Development Environment
Follow the [Building from Source](docs/BUILDING.md) guide to set up your development environment.

### 3. Create a Branch
```bash
# Create and switch to a new branch for your contribution
git checkout -b feature/your-feature-name

# Or for bug fixes
git checkout -b fix/issue-description
```

### 4. Make Your Changes
- **Follow the code style** guidelines (see below)
- **Write or update tests** for your changes
- **Update documentation** if necessary
- **Test thoroughly** on different Android versions when possible

### 5. Commit Your Changes
```bash
# Stage your changes
git add .

# Commit with a descriptive message
git commit -m "Add: Brief description of your changes"
```

### 6. Push and Create Pull Request
```bash
# Push your branch to your fork
git push origin feature/your-feature-name

# Create a pull request on GitHub
```

## 📋 Development Guidelines

### Code Style

#### Kotlin Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use **meaningful variable and function names**
- **Prefer val over var** when possible
- **Use type inference** when the type is obvious

#### Formatting
- Use **Android Studio's default formatting** (Ctrl+Alt+L / Cmd+Option+L)
- **4 spaces for indentation** (no tabs)
- **Maximum line length: 120 characters**
- **Organize imports** and remove unused ones

#### Comments and Documentation
- **Write KDoc comments** for public APIs:
  ```kotlin
  /**
   * Downloads and parses a repository index.
   *
   * @param repository The repository to sync
   * @param force Whether to force a full sync
   * @return The parsed repository data
   */
  suspend fun syncRepository(repository: Repository, force: Boolean): RepositoryData
  ```
- **Add inline comments** for complex logic
- **Use TODO comments** for known issues or future improvements:
  ```kotlin
  // TODO: Implement Index V2 support
  ```

### Architecture Guidelines

#### Follow Clean Architecture
- **Domain layer**: Business logic and models (independent of frameworks)
- **Data layer**: Repository implementations and data sources
- **Presentation layer**: UI components and ViewModels

#### Dependency Injection
- Use **Hilt** for dependency injection
- **Inject dependencies** via constructor injection when possible
- **Create appropriate modules** for different components

#### Testing
- **Write unit tests** for business logic
- **Write integration tests** for complex components
- **Aim for meaningful test coverage**, not just high percentages
- **Use descriptive test names** that explain what is being tested

### Git Workflow

#### Commit Messages
Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no logic changes)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat: add support for custom repository authentication
fix: resolve crash when parsing malformed repository index
docs: update installation instructions in README
refactor: simplify database migration logic
```

#### Branch Naming
Use descriptive branch names that indicate the purpose:
- `feature/add-index-v2-support`
- `fix/installation-crash-android-14`
- `docs/improve-contributing-guide`
- `refactor/simplify-sync-service`

## 🧪 Testing

### Running Tests
```bash
# Run all unit tests
./gradlew test

# Run tests for a specific module
./gradlew app:test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Writing Tests

#### Unit Tests
```kotlin
@Test
fun `parseRepositoryIndex should return valid data for correct input`() {
    // Given
    val inputJson = """{"apps": [...]}"""

    // When
    val result = repositoryParser.parseIndex(inputJson)

    // Then
    assertThat(result).isNotNull()
    assertThat(result.apps).hasSize(expectedAppCount)
}
```

#### Integration Tests
```kotlin
@Test
fun `repository sync should update database with new apps`() = runTest {
    // Given
    val repository = createTestRepository()

    // When
    syncService.syncRepository(repository)

    // Then
    val apps = database.appDao().getAllApps()
    assertThat(apps).isNotEmpty()
}
```

## 📝 Pull Request Guidelines

### Before Submitting
- [ ] **Run tests** and ensure they pass
- [ ] **Update documentation** if needed
- [ ] **Test on actual devices** when possible
- [ ] **Rebase on latest main** branch
- [ ] **Squash commits** if appropriate

### Pull Request Template
When creating a pull request, please include:

1. **Clear title** that summarizes the change
2. **Description** of what was changed and why
3. **Issue reference** (e.g., "Fixes #123" or "Addresses #456")
4. **Testing notes** - how you tested the changes
5. **Screenshots** for UI changes
6. **Breaking changes** if any

### Review Process
- **Maintain patience** - reviews take time
- **Respond to feedback** constructively
- **Make requested changes** promptly
- **Ask questions** if feedback is unclear
- **Update your branch** when requested

## 🏷️ Issue Labels

Understanding our issue labels helps you find issues to work on:

### Type Labels
- `bug` - Something isn't working correctly
- `enhancement` - New feature or improvement
- `documentation` - Documentation related
- `help wanted` - Community help needed
- `good first issue` - Good for newcomers

### Priority Labels
- `critical` - Urgent fixes needed
- `high` - Important issues
- `medium` - Standard priority
- `low` - Nice to have improvements

### Status Labels
- `needs investigation` - Requires research
- `needs design` - Needs UX/UI design work
- `ready for development` - Can be implemented
- `in progress` - Someone is working on it

## 🔐 Security

### Reporting Security Vulnerabilities
**Do not** report security vulnerabilities through public GitHub issues.

Instead, please send an email to [security contact] with:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if any)

See our [Security Policy](SECURITY.md) for more details.

## 📞 Getting Help

### Communication Channels
- **GitHub Issues** - Bug reports and feature requests
- **GitHub Discussions** - General questions and community discussion
- **Weblate** - Translation questions and issues

### Before Asking for Help
1. **Search existing issues** and discussions
2. **Check the documentation** in the `docs/` folder
3. **Review recent commits** for related changes
4. **Try the latest development version** if relevant

## 🎯 Project Priorities

### Current Focus Areas
1. **Index V2 implementation** - Modern repository format support
2. **Performance improvements** - Faster sync and better UX
3. **Test coverage** - More comprehensive testing
4. **Code quality** - Refactoring and modernization

### Areas Needing Help
- **Translation updates** - Keep all languages current
- **Documentation improvements** - Better guides and examples
- **Bug triage** - Help categorize and prioritize issues
- **Testing on different devices** - Ensure broad compatibility

## 📚 Resources

### Android Development
- [Android Developer Documentation](https://developer.android.com/docs)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Material Design Guidelines](https://material.io/design)

### F-Droid Ecosystem
- [F-Droid Documentation](https://f-droid.org/docs/)
- [Repository Format Specification](https://f-droid.org/docs/All_our_APIs/)

### Tools and Libraries
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Room Documentation](https://developer.android.com/jetpack/androidx/releases/room)
- [Ktor Documentation](https://ktor.io/docs/)

## 📜 Code of Conduct

This project adheres to a [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## 🙏 Recognition

Contributors are recognized in several ways:
- **Authors file** - Listed in [AUTHORS.md](AUTHORS.md)
- **Release notes** - Mentioned in release announcements
- **GitHub contributions** - Visible on the repository insights
- **Community appreciation** - Thanks from users and maintainers

---

**Thank you for contributing to Droid-ify!** Your efforts help make open-source software distribution better for everyone. 🚀
