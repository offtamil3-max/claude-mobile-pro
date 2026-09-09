# Claude Mobile Pro 2.0

A mobile-first Claude-style AI workspace using Puter.js for AI. Android wrapper contains no Anthropic API key.

## Build APK

GitHub Actions builds an installable debug APK automatically on pushes to `main`, or manually from **Actions → Build Claude Mobile Pro APK → Run workflow**.

The generated artifact is named `claude-mobile-pro-debug-apk`.

## Features
- Claude-style mobile home screen and composer
- Puter.js Claude chat with streaming
- Correct Puter.js chat invocation with options in the third argument
- Live model selector with fallback handling
- File picker attachment bridge
- Tamil native voice input bridge
- Local chats/projects/plugins/checkpoints
- GitHub OAuth bridge and repository listing
- Mobile-first dark UI
- Android WebView shell
- HTTPS appassets origin for Puter.js compatibility

## Limitations
This is a working foundation. Full GitHub CRUD, deployment adapters, sandboxed third-party plugin execution, native attachment content extraction, and a full code editor still need dedicated implementation.

Latest fix: Puter AI chat invocation and model fallback.
