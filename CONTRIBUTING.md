# 🤝 Contributing to Droid-ify

Thank you for your interest in contributing to Droid-ify! This document provides guidelines for contributors.

## 🌟 Ways to Contribute

### 🐛 Bug Reports
- Search existing issues before creating new ones
- Use bug report templates when available
- Include system information and reproduction steps
- Add screenshots when applicable

### 💡 Feature Requests
- Check for existing requests to avoid duplicates
- Clearly describe the feature and its use case
- Explain why it would be valuable

### 🔧 Code Contributions
- **Bug fixes** - Fix reported issues
- **New features** - Implement requested features
- **Performance improvements** - Optimize existing code
- **Code refactoring** - Improve code quality

### 🌐 Translations
Help translate Droid-ify via [Weblate](https://hosted.weblate.org/engage/droidify/)

### 📖 Documentation
Improve README, guides, and code comments

## 🚀 Getting Started

1. **Fork and clone** the repository
2. **Set up environment** following [Building from Source](docs/building.md)
3. **Create a branch** for your contribution
4. **Make changes** following our guidelines
5. **Test thoroughly** on different Android versions
6. **Create a pull request**

## 📋 Development Guidelines

### Code Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Prefer `val` over `var` when possible
- Use Android Studio's default formatting (Ctrl+Alt+L)
- 4 spaces for indentation, 120 character line limit
- Organize imports and remove unused ones

### Architecture
- Follow Clean Architecture principles
- **Domain layer**: Business logic and models
- **Data layer**: Repository implementations and data sources
- **Presentation layer**: UI components and ViewModels
- Use Hilt for dependency injection
- Write meaningful tests for business logic

### Git Workflow
Use [Conventional Commits](https://www.conventionalcommits.org/) format:
```
<type>: <description>
```

**Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `chore`

**Branch naming**:
- `feature/description`
- `fix/description`
- `docs/description`

## 📝 Pull Request Guidelines

### Before Submitting
- [ ] Run tests and ensure they pass
- [ ] Update documentation if needed
- [ ] Test on actual devices when possible
- [ ] Rebase on latest main branch

### Pull Request Content
- Clear title summarizing the change
- Description of what was changed and why
- Issue reference (e.g., "Fixes #123")
- Testing notes and screenshots for UI changes

## 🏷️ Issue Labels

| Type | Description |
|------|-------------|
| `bug` | Something isn't working |
| `enhancement` | New feature or improvement |
| `documentation` | Documentation related |
| `help wanted` | Community help needed |
| `good first issue` | Good for newcomers |

| Priority | Description |
|----------|-------------|
| `critical` | Urgent fixes needed |
| `high` | Important issues |
| `medium` | Standard priority |
| `low` | Nice to have |

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

## 🔐 Security

**Do not** report security vulnerabilities through public GitHub issues. Send details to [security contact] instead.

## 📞 Getting Help

- **GitHub Issues** - Bug reports and feature requests
- **GitHub Discussions** - General questions
- **Weblate** - Translation questions

Search existing issues and documentation before asking for help.

## 📚 Resources

- [Android Developer Documentation](https://developer.android.com/docs)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [F-Droid Documentation](https://f-droid.org/docs/)
- [Hilt Documentation](https://dagger.dev/hilt/)

## 📜 Code of Conduct

This project adheres to a [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

---

**Thank you for contributing to Droid-ify!** 🚀
