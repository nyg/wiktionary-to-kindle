# Changelog


## [2.2.0](https://github.com/nyg/wiktionary-to-kindle/compare/v2.1.0..v2.2.0) - 2026-08-16

### ⛰️  Features

- [`a35611e`](https://github.com/nyg/wiktionary-to-kindle/commit/a35611ec4df9f73f551b9e1efbadf15a9774114d) *(release)* Ship deb, rpm and a Windows exe installer ([#93](https://github.com/nyg/wiktionary-to-kindle/issues/93))

### 🚜 Refactor

- [`bcc0c81`](https://github.com/nyg/wiktionary-to-kindle/commit/bcc0c81032afb616dfc218b5ae9e710d9afd9377) Clear the low-effort sonar findings ([#94](https://github.com/nyg/wiktionary-to-kindle/issues/94))

## [2.1.0](https://github.com/nyg/wiktionary-to-kindle/compare/v2.0.3..v2.1.0) - 2026-08-15

### ⛰️  Features

- [`b3272a1`](https://github.com/nyg/wiktionary-to-kindle/commit/b3272a1289a7d4a3727f99634db41d016fdf8c4a) Tidy the dictionaries folder and roll the log daily ([#91](https://github.com/nyg/wiktionary-to-kindle/issues/91))
- [`aae8822`](https://github.com/nyg/wiktionary-to-kindle/commit/aae8822b54ebce57739ae3dd58cfa0dadb85f517) *(gui)* Add AtlantaFX Cupertino themes ([#89](https://github.com/nyg/wiktionary-to-kindle/issues/89))
- [`e1a6e16`](https://github.com/nyg/wiktionary-to-kindle/commit/e1a6e1682b148a0db0567526901fac57f1d2e649) Source both language pickers from kaikki and stop accepting free text ([#84](https://github.com/nyg/wiktionary-to-kindle/issues/84))
- [`da77960`](https://github.com/nyg/wiktionary-to-kindle/commit/da77960b6faaa777b5242518bc0aac4a6f3f64b4) *(cli)* **[breaking]** Share the desktop app's data folders ([#87](https://github.com/nyg/wiktionary-to-kindle/issues/87))

### 🐛 Bug Fixes

- [`0e819bf`](https://github.com/nyg/wiktionary-to-kindle/commit/0e819bf7af38173aec2a4de2bcc9b8fe4adc18db) *(gui)* Resolve misc UI defects ([#90](https://github.com/nyg/wiktionary-to-kindle/issues/90))
- [`540a9dd`](https://github.com/nyg/wiktionary-to-kindle/commit/540a9dd1423b491db2ffbe8da413ee27b773f69a) *(config)* Enforce the absolute-path invariant on data directories ([#88](https://github.com/nyg/wiktionary-to-kindle/issues/88))
- [`990da1a`](https://github.com/nyg/wiktionary-to-kindle/commit/990da1afbd027ee1e2d0ef3bd402d0ec78e2ae65) *(gui)* Resolve typed edition names to codes ([#83](https://github.com/nyg/wiktionary-to-kindle/issues/83))

### ⚙️ Miscellaneous

- [`6000655`](https://github.com/nyg/wiktionary-to-kindle/commit/6000655e0c4e0c9fc6e07675cc749d7ee644ba99) Improve PR body for kindling-cli update ([#82](https://github.com/nyg/wiktionary-to-kindle/issues/82))
- [`a28f73e`](https://github.com/nyg/wiktionary-to-kindle/commit/a28f73e8c4179d3c07d08d3daf3310ebe543a000) *(deps)* Update kindling-cli to v0.31.0 ([#81](https://github.com/nyg/wiktionary-to-kindle/issues/81))
- [`5c82aed`](https://github.com/nyg/wiktionary-to-kindle/commit/5c82aed02507e23835263a15dd2a37c8dfc6aa9c) Retry GitHub release creation on transient API failures ([#78](https://github.com/nyg/wiktionary-to-kindle/issues/78))

## [2.0.3](https://github.com/nyg/wiktionary-to-kindle/compare/v2.0.2..v2.0.3) - 2026-08-03

### ⚙️ Miscellaneous

- [`45b9f83`](https://github.com/nyg/wiktionary-to-kindle/commit/45b9f83323b4d229152ff2dd7acd81e7db4ed315) Use conventional commit messages for release commits ([#77](https://github.com/nyg/wiktionary-to-kindle/issues/77))

## [2.0.2](https://github.com/nyg/wiktionary-to-kindle/compare/v2.0.1..v2.0.2) - 2026-08-03

### ⛰️  Features

- [`9c0bc56`](https://github.com/nyg/wiktionary-to-kindle/commit/9c0bc56a6f698e889c7414ff4d7e31b5a900eb10) Harmonize the app name, follow XDG, prefix dictionaries with "W2K" ([#76](https://github.com/nyg/wiktionary-to-kindle/issues/76))

## [2.0.1](https://github.com/nyg/wiktionary-to-kindle/compare/v2.0.0..v2.0.1) - 2026-08-01

### 🐛 Bug Fixes

- [`2df9887`](https://github.com/nyg/wiktionary-to-kindle/commit/2df9887998f539e5799960eaaea4389ad27d59a0) *(deps)* Update all stable non-major dependencies ([#73](https://github.com/nyg/wiktionary-to-kindle/issues/73))

### 📚 Documentation

- [`8899b20`](https://github.com/nyg/wiktionary-to-kindle/commit/8899b2057c695fe985d6c51d6f6d238ad6b5be88) Lead README with a screenshot; fix blank columns in the dumps table ([#74](https://github.com/nyg/wiktionary-to-kindle/issues/74))

### ⚙️ Miscellaneous

- [`bfa102a`](https://github.com/nyg/wiktionary-to-kindle/commit/bfa102a2a95e237ad0a860d554c19aec6ca0b664) *(deps)* Update kindling-cli to v0.29.1 ([#75](https://github.com/nyg/wiktionary-to-kindle/issues/75))

## 2.0.0 - 2026-07-28

### ⛰️  Features

- [`987ea68`](https://github.com/nyg/wiktionary-to-kindle/commit/987ea68daf42e28ad5afc172c115790a22e18253) *(gui)* Add JavaFX desktop app over the existing pipeline ([#67](https://github.com/nyg/wiktionary-to-kindle/issues/67))
- [`a94ae70`](https://github.com/nyg/wiktionary-to-kindle/commit/a94ae70a56ff526f496ce3fa138eaa074ccb60c0) *(config)* Add app paths, preferences and dump catalog for the GUI ([#66](https://github.com/nyg/wiktionary-to-kindle/issues/66))
- [`2f933d5`](https://github.com/nyg/wiktionary-to-kindle/commit/2f933d5a868f5a84c225cf96b0189f90ce718516) *(progress)* Add progress and cancellation seams to the pipeline ([#65](https://github.com/nyg/wiktionary-to-kindle/issues/65))
- [`7aef375`](https://github.com/nyg/wiktionary-to-kindle/commit/7aef3752854ba2c88eee46d996033edc230c9c79) Inflected word forms (declensions/conjugations) ([#48](https://github.com/nyg/wiktionary-to-kindle/issues/48))
- [`ec64cde`](https://github.com/nyg/wiktionary-to-kindle/commit/ec64cde3486099a0749e3aa8efb5d79fc0726c77) Date-based dump naming with auto-discovery ([#42](https://github.com/nyg/wiktionary-to-kindle/issues/42))
- [`15428a7`](https://github.com/nyg/wiktionary-to-kindle/commit/15428a743f94c07c332c59e920d2677efed2ed59) Delete old static utility classes and their tests ([#37](https://github.com/nyg/wiktionary-to-kindle/issues/37))
- [`60d0870`](https://github.com/nyg/wiktionary-to-kindle/commit/60d087076d62f18610299446fc05629011553e80) Introduce Command pattern and refactor CLI as composition root ([#36](https://github.com/nyg/wiktionary-to-kindle/issues/36))
- [`1b6edb8`](https://github.com/nyg/wiktionary-to-kindle/commit/1b6edb864a880fc4e3e3556e0ec92caa29c8f628) Introduce OpfGenerator interface and KindleOpfGenerator ([#35](https://github.com/nyg/wiktionary-to-kindle/issues/35))
- [`f83d4c3`](https://github.com/nyg/wiktionary-to-kindle/commit/f83d4c3c7100463ffef5ceba51fdb7c7435e4b82) Introduce LexiconReader interface and TsvLexiconReader ([#34](https://github.com/nyg/wiktionary-to-kindle/issues/34))
- [`66e8045`](https://github.com/nyg/wiktionary-to-kindle/commit/66e80454336dd5e127d8cbf762f99d68306766fc) Introduce LexiconEntry record, LexiconWriter interface, TsvLexiconWriter ([#30](https://github.com/nyg/wiktionary-to-kindle/issues/30))
- [`6578fe8`](https://github.com/nyg/wiktionary-to-kindle/commit/6578fe8475b59a92e50dbdc0b0bbbede8febd993) Introduce DumpDownloader interface and KaikkiDumpDownloader ([#29](https://github.com/nyg/wiktionary-to-kindle/issues/29))
- [`d50f16b`](https://github.com/nyg/wiktionary-to-kindle/commit/d50f16b59ce064cc8b664731bfdc3e5d19f54405) Introduce DictionaryParser interface and JsonlDictionaryParser ([#32](https://github.com/nyg/wiktionary-to-kindle/issues/32))
- [`3ffc708`](https://github.com/nyg/wiktionary-to-kindle/commit/3ffc708b1514b04b341e66d8c8602a06e1ff4fc3) Introduce DefinitionRenderer interface and HtmlDefinitionRenderer ([#31](https://github.com/nyg/wiktionary-to-kindle/issues/31))

### 🐛 Bug Fixes

- [`7f83ba0`](https://github.com/nyg/wiktionary-to-kindle/commit/7f83ba0f428da07b78ce31ef29df2d8ed7845a27) *(deps)* Update javafx monorepo to v26 ([#68](https://github.com/nyg/wiktionary-to-kindle/issues/68))
- [`1efc9c8`](https://github.com/nyg/wiktionary-to-kindle/commit/1efc9c8f454ec8283376d1dbe853b45be1651ca8) Resolve inflected-form lookups to their lemma entry ([#57](https://github.com/nyg/wiktionary-to-kindle/issues/57))
- [`a54c159`](https://github.com/nyg/wiktionary-to-kindle/commit/a54c1595b6ed32baaa11f5c424f0b082ffbcf670) Don't time out multi-gigabyte dump downloads ([#58](https://github.com/nyg/wiktionary-to-kindle/issues/58))
- [`55a8745`](https://github.com/nyg/wiktionary-to-kindle/commit/55a8745a719f08e1000931225b9268760997431a) *(deps)* Update all stable non-major dependencies ([#54](https://github.com/nyg/wiktionary-to-kindle/issues/54))
- [`e83f973`](https://github.com/nyg/wiktionary-to-kindle/commit/e83f973a455a88bb72e292781eabc161ec696065) *(deps)* Update all stable non-major dependencies ([#49](https://github.com/nyg/wiktionary-to-kindle/issues/49))

### 🚜 Refactor

- [`630d012`](https://github.com/nyg/wiktionary-to-kindle/commit/630d01284034d41b44533af612d8a6948661c537) *(kindling)* Load pinned release from a properties resource ([#60](https://github.com/nyg/wiktionary-to-kindle/issues/60))
- [`3980fb0`](https://github.com/nyg/wiktionary-to-kindle/commit/3980fb083d9229d2d61dd8c533ff85e51a12afe4) Replace kindlegen by kindling for.mobi generation ([#44](https://github.com/nyg/wiktionary-to-kindle/issues/44))
- [`b7a821b`](https://github.com/nyg/wiktionary-to-kindle/commit/b7a821b0ed3b7cee5452668cc35c7aeafb039265) Streaming parser, records, Optional renderer, remove TSV step, non-English download ([#41](https://github.com/nyg/wiktionary-to-kindle/issues/41))

### 📚 Documentation

- [`fc794dc`](https://github.com/nyg/wiktionary-to-kindle/commit/fc794dc2e63efd5b158a9db6afc92ad2d2358c56) Rewrite for the desktop app ([#72](https://github.com/nyg/wiktionary-to-kindle/issues/72))
- [`6e4f588`](https://github.com/nyg/wiktionary-to-kindle/commit/6e4f588318d2c43def949f6d8657d4f28e2d852b) Fix Mermaid diagram syntax in program-flow ([#39](https://github.com/nyg/wiktionary-to-kindle/issues/39))
- [`2b16c86`](https://github.com/nyg/wiktionary-to-kindle/commit/2b16c863d51a956e6fb7fc5225c280e860b64d23) Update README and program-flow for OOP architecture ([#38](https://github.com/nyg/wiktionary-to-kindle/issues/38))
- [`4dab47a`](https://github.com/nyg/wiktionary-to-kindle/commit/4dab47ab4fa6fdf9a81f21daef7a5fa41162513e) Add dual-model AI code review ([#19](https://github.com/nyg/wiktionary-to-kindle/issues/19))

### 🧪 Testing

- [`5ce6349`](https://github.com/nyg/wiktionary-to-kindle/commit/5ce6349d53283ebdca604528e47acec0ba3bc721) Rewrite test suite with Mockito + AssertJ ([#45](https://github.com/nyg/wiktionary-to-kindle/issues/45))
- [`def807d`](https://github.com/nyg/wiktionary-to-kindle/commit/def807da520c450ab4537c4fc951a59b79a59359) Integration tests for full generate pipeline + program flow diagram ([#18](https://github.com/nyg/wiktionary-to-kindle/issues/18))

### ⚙️ Miscellaneous

- [`037fde3`](https://github.com/nyg/wiktionary-to-kindle/commit/037fde38d21a26c7878b7d30f0a5d993c7e9bf98) Add the release workflow and bump to 2.0.0-SNAPSHOT ([#71](https://github.com/nyg/wiktionary-to-kindle/issues/71))
- [`a21f8e4`](https://github.com/nyg/wiktionary-to-kindle/commit/a21f8e4d69b19a60ab15dc314db5058d97b9dd07) Bundle the app with jlink and jpackage ([#70](https://github.com/nyg/wiktionary-to-kindle/issues/70))
- [`0989d9c`](https://github.com/nyg/wiktionary-to-kindle/commit/0989d9c110a0cbb0e26dd2c413f17862625cb574) Auto-merge kindling update PRs when required checks pass ([#64](https://github.com/nyg/wiktionary-to-kindle/issues/64))
- [`41a7a95`](https://github.com/nyg/wiktionary-to-kindle/commit/41a7a950e121fa48c093bc5a09301964034520b7) *(deps)* Update kindling-cli to v0.28.0 ([#63](https://github.com/nyg/wiktionary-to-kindle/issues/63))
- [`65e9ace`](https://github.com/nyg/wiktionary-to-kindle/commit/65e9acee731f4b8d2d519b152cfe062442c1e6af) *(deps)* Update peter-evans/create-pull-request action to v8 ([#62](https://github.com/nyg/wiktionary-to-kindle/issues/62))
- [`6972bd6`](https://github.com/nyg/wiktionary-to-kindle/commit/6972bd664457ae5d1804b048e65ea17b91361f89) Automate kindling-cli release updates ([#61](https://github.com/nyg/wiktionary-to-kindle/issues/61))
- [`4f069a5`](https://github.com/nyg/wiktionary-to-kindle/commit/4f069a59db5a0e66b806dbc9fbc746a68bfdeffc) *(deps)* Pin actions/checkout action to v7.0.0 ([#55](https://github.com/nyg/wiktionary-to-kindle/issues/55))
- [`0d0c233`](https://github.com/nyg/wiktionary-to-kindle/commit/0d0c23398eb1f6e11800e43527676b0f0e6dd6ce) *(deps)* Update actions/checkout action to v7 ([#53](https://github.com/nyg/wiktionary-to-kindle/issues/53))
- [`9ea960b`](https://github.com/nyg/wiktionary-to-kindle/commit/9ea960b53fa0213fc09322ed21df20db9d45a1a0) *(deps)* Pin dependencies ([#50](https://github.com/nyg/wiktionary-to-kindle/issues/50))
- [`63099c5`](https://github.com/nyg/wiktionary-to-kindle/commit/63099c5c194c4419afce7824ad41779773cb8e93) Skip sonar when SONAR_TOKEN is unavailable ([#52](https://github.com/nyg/wiktionary-to-kindle/issues/52))
- [`3573c28`](https://github.com/nyg/wiktionary-to-kindle/commit/3573c28e248fb06accd7fc37a4c3c596c462d839) *(deps)* Update actions/checkout digest to df4cb1c ([#51](https://github.com/nyg/wiktionary-to-kindle/issues/51))
- [`c677583`](https://github.com/nyg/wiktionary-to-kindle/commit/c6775839c0d83d7ae616e2d583dd5e1b1f3ddd1f) Misc changes ([#46](https://github.com/nyg/wiktionary-to-kindle/issues/46))
- [`d09c1e2`](https://github.com/nyg/wiktionary-to-kindle/commit/d09c1e2721a27fda9cb0fd29e142eaf89d642ed8) Add Sonar analysis ([#43](https://github.com/nyg/wiktionary-to-kindle/issues/43))
- [`1058993`](https://github.com/nyg/wiktionary-to-kindle/commit/10589936122300456b67c115ccdeffd9cbef5b38) Migrate to picocli ([#40](https://github.com/nyg/wiktionary-to-kindle/issues/40))

### Others

- [`87e0970`](https://github.com/nyg/wiktionary-to-kindle/commit/87e0970b2740744db75aa1a7b6f022bdbb2130e1) Update dependency com.fasterxml.jackson.core:jackson-databind to v2.21.3 ([#33](https://github.com/nyg/wiktionary-to-kindle/issues/33))
- [`5ee3a98`](https://github.com/nyg/wiktionary-to-kindle/commit/5ee3a98837b1db229a8de17f98886820820e500e) Replace tab2opf Python submodule with Java OPF generator ([#17](https://github.com/nyg/wiktionary-to-kindle/issues/17))
- [`b3907f0`](https://github.com/nyg/wiktionary-to-kindle/commit/b3907f08e2099e084a70607b44402cb47963c913) Update all stable non-major dependencies ([#13](https://github.com/nyg/wiktionary-to-kindle/issues/13))
- [`0c59802`](https://github.com/nyg/wiktionary-to-kindle/commit/0c5980203133a0108810b4e576e314a9f4a50a93) Add Java CI workflow ([#16](https://github.com/nyg/wiktionary-to-kindle/issues/16))
- [`c5e2af9`](https://github.com/nyg/wiktionary-to-kindle/commit/c5e2af96c7e6b40a217feed19fe23ba511555151) Update dependency org.junit.jupiter:junit-jupiter to v6 ([#14](https://github.com/nyg/wiktionary-to-kindle/issues/14))
- [`32b8db0`](https://github.com/nyg/wiktionary-to-kindle/commit/32b8db0a386b0d28c3bad971c360d171fb1142ff) Upgrade to Java 25, add Lombok + SLF4J/Logback, update Maven plugins ([#12](https://github.com/nyg/wiktionary-to-kindle/issues/12))
- [`9b9b9ec`](https://github.com/nyg/wiktionary-to-kindle/commit/9b9b9ec68a9c3793001abccbe4306e7b374eb19a) Replace JWKTL with kaikki.org pre-extracted JSONL ([#11](https://github.com/nyg/wiktionary-to-kindle/issues/11))
- [`a8a11cf`](https://github.com/nyg/wiktionary-to-kindle/commit/a8a11cfda263f5104de1ca8f5c2884da0c597aba) Add copilot instructions
- [`18895fe`](https://github.com/nyg/wiktionary-to-kindle/commit/18895fe21ced5c2d1032945273cf8724c4c1ad56) Update dependencies and Java version
- [`ba6f180`](https://github.com/nyg/wiktionary-to-kindle/commit/ba6f1800a303145dbefbf211e4df51218f1df5c3) Remove limit when parsing XML file
- [`af2775d`](https://github.com/nyg/wiktionary-to-kindle/commit/af2775dde47c1b7e858bba704196b1dba395615f) Avoid throwing exception when page cannot be saved
- [`b99a18b`](https://github.com/nyg/wiktionary-to-kindle/commit/b99a18b5706904b81ebba25d05de9246836ba417) Ignore lex and epub-test folders
- [`6f3ed2b`](https://github.com/nyg/wiktionary-to-kindle/commit/6f3ed2be698f75669697af57384504d8f56d27e6) Update documentation.
- [`3ad206c`](https://github.com/nyg/wiktionary-to-kindle/commit/3ad206cb21b8b0ac4a7cb4ad914e90b37fc7abbe) Bump tab2opf module.
- [`90996ba`](https://github.com/nyg/wiktionary-to-kindle/commit/90996ba4cbbcdb03d076cbfa4c9ef2030a6c15cf) Use findByCode instead of get.
- [`7c9570f`](https://github.com/nyg/wiktionary-to-kindle/commit/7c9570f8af05ae834a46e57c17fba538221bf08e) Escape definitions in XML documents.
- [`40c8e42`](https://github.com/nyg/wiktionary-to-kindle/commit/40c8e42da63196aac87d7c2e00591ba22d12b0e1) Import order.
- [`3748618`](https://github.com/nyg/wiktionary-to-kindle/commit/3748618214ffdf7ea6b324514dd437cfb525cab6) Bump tab2opf module.
- [`ca90c93`](https://github.com/nyg/wiktionary-to-kindle/commit/ca90c93d7647f347835a28f9a828680482596843) Add helpful documentation links.
- [`46657c8`](https://github.com/nyg/wiktionary-to-kindle/commit/46657c8cbfe2239dfc7100de7fff47bae5c1e0e1) Fixed path of kindlegen when executed from another directory.
- [`e0a0ac5`](https://github.com/nyg/wiktionary-to-kindle/commit/e0a0ac5d71ac37347e0aa98fc8cd8d5cdd5695ab) Updated README.
- [`c29c00a`](https://github.com/nyg/wiktionary-to-kindle/commit/c29c00a64db8992ae1825c63c7b3ce72ab5cfb5f) Renamed kindlegen_win to kindlegen_windows.
- [`16542cc`](https://github.com/nyg/wiktionary-to-kindle/commit/16542ccffeeca2fb4cb95148263ec2163be3420d) Added kindlegen for 64-bit macOS (https://www.literatureandlatte.com/forum/viewtopic.php\?p\=284621\#p284621).
- [`b16a817`](https://github.com/nyg/wiktionary-to-kindle/commit/b16a817e8a2570208de24e38a6c7781f628d1bc8) Also parse pages in the reconstruction namespace.
- [`aadc06f`](https://github.com/nyg/wiktionary-to-kindle/commit/aadc06fd7bb44a53b0e93e750e8e5caf93cbdab2) Updated requirements to Java 11 and updated gitignore.
- [`9ef1c22`](https://github.com/nyg/wiktionary-to-kindle/commit/9ef1c221a406d8d49292616793fdb27e4e9b2cf3) Display error if language code doesn't exists when generating dictionary.
- [`4458ec9`](https://github.com/nyg/wiktionary-to-kindle/commit/4458ec9cf03ed69a77cb7e8b0dd32d98b44a8285) Override JWKTL language_codes list and added gem-pro.
- [`31cd096`](https://github.com/nyg/wiktionary-to-kindle/commit/31cd0963073c21fdcb17bddfd92b88c3f8b181b0) Updated tab2opf submodule.
- [`6061346`](https://github.com/nyg/wiktionary-to-kindle/commit/606134689b681d27f02bf0b20a346f4b9962ab84) Replaced use of Paths.get by non OS-dependent methods.
- [`e651616`](https://github.com/nyg/wiktionary-to-kindle/commit/e6516160ee3b623e6bb4ab368164c3aed8eb0f8f) Set charset for lexicon.txt file to UTF-8, removed use of StringBuilder and write directly to file.
- [`2d75b9e`](https://github.com/nyg/wiktionary-to-kindle/commit/2d75b9ee891a1beee2971a61c9dd63d83da4a161) Updated org.apache.commons commons-compress.
- [`1e70982`](https://github.com/nyg/wiktionary-to-kindle/commit/1e709826d298ba90536489cfea5df3f296a712ad) Updated commons-compress.
- [`6cb983c`](https://github.com/nyg/wiktionary-to-kindle/commit/6cb983c8a3666897c2e8f606eb0781178c35cbec) Removed kindlegen help files.
- [`04b98a2`](https://github.com/nyg/wiktionary-to-kindle/commit/04b98a27eecf38cdaa5175f68751a3e65e2d5b70) Added dl links to generated dictionaries.
- [`32a8691`](https://github.com/nyg/wiktionary-to-kindle/commit/32a86916c0c17522854330677091ff0d9885c697) Merge branch 'master' of github.com:nyg/wiktionary-to-kindle
- [`8fe0059`](https://github.com/nyg/wiktionary-to-kindle/commit/8fe005912cc1089443a9aaf60c259b9f7e5bb571) Typos
- [`24a8bf9`](https://github.com/nyg/wiktionary-to-kindle/commit/24a8bf977c4cfedfcf81381c8d63fc301e1aa823) Escape dollar sign in templates, otherwise they are considered to represent a regex group.
- [`f3b4719`](https://github.com/nyg/wiktionary-to-kindle/commit/f3b471924ec63081a2bf7d5bf2d166865c34d9d5) Remove new lines from getGloss and getExamples, they happen when a word has sub-definitions... Also print template content if they can't be parsed.
- [`702d8bb`](https://github.com/nyg/wiktionary-to-kindle/commit/702d8bb8106b640e67cc3f3027b52110967c6c4e) Added how to instructions.
- [`1c89b46`](https://github.com/nyg/wiktionary-to-kindle/commit/1c89b4676ab90fe596a5570bad16e832ba2463fa) Fixed POM so the jar can be executed from the command line.
- [`a85dd71`](https://github.com/nyg/wiktionary-to-kindle/commit/a85dd7108d9b138dc63d34756fb9e80d02664b4c) Added project workflow to README and precisions to LICENSE.
- [`f38b9db`](https://github.com/nyg/wiktionary-to-kindle/commit/f38b9db6aacc47bf8f64ebc3a205cb3ce3277f8d) Updating tab2opf submodule.
- [`d959f11`](https://github.com/nyg/wiktionary-to-kindle/commit/d959f11d8fe864885967395ceb535f26483e0afe) Imported my fork of tab2opf
- [`b8b4bb0`](https://github.com/nyg/wiktionary-to-kindle/commit/b8b4bb04e19849a74477403be7a494dd530479c0) Added kindlegen executables
- [`d42d066`](https://github.com/nyg/wiktionary-to-kindle/commit/d42d066f3f0432399820a2c9dd2075f2d91b3154) Imported project.
- [`7ba54b6`](https://github.com/nyg/wiktionary-to-kindle/commit/7ba54b6b52f7cc93671a6dd2b22ee19093264d5f) Added some empty folders.
- [`f49a409`](https://github.com/nyg/wiktionary-to-kindle/commit/f49a4093dc5a9ac24eef596a8f94c5bfc9dc105d) Proper .gitignore
- [`f145dc6`](https://github.com/nyg/wiktionary-to-kindle/commit/f145dc6b54ed2a5a840470dfcc835b081b12cc88) Initial commit

<!-- generated by git-cliff -->
