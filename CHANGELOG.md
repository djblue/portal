## 0.53.0 - 2024-03-22

- Add `portal.viewer/code` helper 7a3b67e
- Get selected values for all sessions 6f8c4a5
- Revert `org.clojure/data.json` to fix transitive issues in bb 92c0258
- Add `clojure.zip` to sci cf1bf85
- Highlight context on mouse hover 4848083
- Improve `portal.runtime` async error handling 57e9e62
- Initial support for user specified shortcuts f76a0ed

## 0.52.2 - 2024-02-17

- Fix deploys

## 0.52.1 - 2024-02-17

- Bump deep-diff to preserve metadata

## 0.52.0 - 2024-02-17

- Register `portal.api/register!` as a command 6a66bc7
- Get Portal runtime working in Joyride (#204) a60396d
- Allow passing theme for embedded view 8f09472
- Add support for `:column` metadata for coll-of-vector tables 2a55d25

### Docs

- Replace t/report with t/do-report (#209) 2b2a18e
  - Thanks @mrkam2!
- Document limitations with doubles 4ecccaf

### Bug Fixes

- Improve `portal.client.*` error handling 8550bf2
- Fix `portal.runtime.node.server` session-id issue 2d81be8
- Fix: the environment variable `PATH` might be undefined. In this case
  `clojure.string/split` fails since the first argument is `nil`. (#210) a30264a
  - Thanks @maxweber!
- Prevent body background-color from being set in embedded mode ae9017a
- Scope `highlight.js` classes to code viewer 0060083

## 0.51.1 - 2024-01-07

- Add ubuntu's chromium-browser exe to list of browsers (#207) ed2b8a1
  - Thanks @mrnhrd for the PR!
- Remove hash-able requirement for runtime objects 486bcc5
  - Thanks @seancorfield for isolating this issue! (#208)

## 0.51.0 - 2023-12-12

- Add `size-bytes` viewer 8ce9813
- Updates IJ plugin for 2023.3 (#205) 006da80
  - Thanks @coyotesqrl!
- Disable socket timeout for node server 94e359f
- Include larger units in duration viewer e0983f6

## 0.50.0 - 2023-12-04

- Provide better open-file support for vs-code 16c4a91
- Add `:format/jwt` for paste command a7cf814
- JWT viwer, show nbf as date-time by default (#202) 85bfd11
  - Thanks @handerpeder!
- Implement namespaced maps for `portal.inspector` 939b33a
- Add toggle-watch to deref viewer 5ce1773
- Add `toggle-watch` command 8c1cb5b
- Add `portal.web/inspect` bd5332e

### Bug Fixes

- Lower stack-trace viewer priority 30a2438
- Fix test-report viewer edge case a566265
- Fix serialization for new cljs bigint changes 5762ba7

## 0.49.1 - 2023-11-07

- Make docs more responsive 326e8a8
- Make markdown and hiccup viewer css more responsive 9c0e968
- Improve default hiccup styles 47e6fd9
- Improve prepl viewer styles 7786680
- Improve exception viewer styles ed7b4ad

## 0.49.0 - 2023-10-30

- Add more viewer fns to portal.viewer 062f3a8
- Improve style parsing for markdown viewer 65e0873
- Better vscode theme color mapping 4d6cd8f

### New Features

- Adds portal.api/selected 2984451
  - Thanks @handerpeder!
- Support multi-selection in copy and copy-json commands 1eb0f69
- Add runtime icon indicator 3e6b60e
- Add `:launcher :iframe` as flag for portal.web 3d5d5a5
  - Thanks @FelipeCortez !

### Documentation

- Add video presentations to docs 51e5756
- Update emacs.md with a guide for Monroe and xwidget brower 88f7ce5
  - Thanks @lukaszkorecki!
- Update default viewer doc to use portal.viewer ns b939f75
- Document table viewer options 0ec96a5

### Bug Fixes

- Fix rendering bug where maps contain `:key` key 89b381d
- Fix toggle-expand for string / objects d0def02

## 0.48.0 - 2023-09-18

- Prefer `umd` export for npm modules 86b2b61
- Expose portal embed view via `js/portal_api` 5f8d51a
- Add `clojure.walk/keywordize-keys` to commands 5fb3bd3

### Documentation

- Add limitation docs around lazy values 6a0853a
- Add vega-lite concat chart examples 7966410

### Bug Fixes

- Add gaurds to tap-list around bad lazy values a56b786
- Fix vega-lite examples 718b5a4
- Fix `:key` issue in exception viewer 111725d

## 0.47.0 - 2023-09-14

- Remembers state of UI on change (#194) fbc5463
  - Thanks @Cyrik!
- Initial commit of portal.viewer 2d6762c
- Add portal.client.dart (#193) 54d7c9d
- Allow cljs to read non-standard edn symbols 0b35b6a
  - Thanks @Lambeaux for the bug report!

### Vega

- Improve vega styles 6e71096
- Improve vega responsive sizing logic a7de0e9
- Re-render vega viewer on theme change 268b131
- Improve vega theme integration ae240b1

### Documentation

- Improve `portal.api` docs 9d62ccb
- Start documenting limitations af37b6f

## 0.46.0 - 2023-08-17

### Improvements

- Add `:format/csv` as a paste-as format dfc84d0
- Support vega-lite :datasets as well as :data (#192) 82adea3
  -  Thanks @athos!
- Prefer selected viewer over default in metadata fcaec0e
- Improve runtime re-connection mechanism 74b0099
- Update node runtime client to wait for session 750d3fa

### Documentation

- Update docs to include binary data b0413f6
- Build docs into standalone app 08b2022
- Update viewer docs with more examples fab6637

### Bug Fixes

- Fix runtime object equality 88708c3
- Fix `can-meta?` predicate b9c7e33
- Fix viewer context bug from #191 c49f649
- Fix socket tracking for node runtime 8a57602
- Fix `portal.runtime.node.launcher/eval-str` d2bf42c
- Fix `portal.runtime.node.launcher/sessions` 7d09451
- Fix node ipv6 default for localhost e1e9324
- Localhost fix for node `ECONNREFUSED` d3fc041

## 0.45.1 - 2023-08-01

- Improve ex viewer stack trace parsing

## 0.45.0 - 2023-07-31

- Use source location viewer in test report viewer bd884f6
- Next iteration of tree viewer d228657
- Multi selection updates Thanks @MarlonCorreia!
  - Add multi selection change from viewers embed commands 1f11c05
  - Add multi selection change from viewers commands e8b363a
  - Add multi selection change from viewers list 18289b3
- Prefer :ns when resolving source location 5297dff
- Next iteration of the exception viewer 8a299f4
- Use :file as fallback for source-location viewer 30c2dbe
- Capture expanded on history-push 3d7c8e9
- Improve goto-definition a4dd8f1
  - Set :auto for :editor option in goto-definition c9c174f
- Updates IJ plugin for 2023.2 (#190) b2d2ad5 - Thanks @coyotesqrl!

## 0.44.0 - 2023-07-25

- Add expand / collapse actions to prepl title bar 0c6ca2c
- Shift + click expand icon to expand-inc cf14231
- Add tufte guide 223d49f
- Add source-location viewer faf88c1
- Add duration-ms and duration-ns viewers fed87f6
- Improve filtering on various viewers 10d0145
- Enable edn parsing containing regex 381017b

## 0.43.0 - 2023-07-23

- Add expand-inc for expanding children from parent (#189) d117347
- Add heuristic for unix time stamps encoded as ms d4167b0
- Improve goto fallback for symbols 30c7a3b
- Enable :portal.viewer/for in table / diff viewers ee449af

### Documentation

- Fix some typos (#188) 13f408a - Thanks @dakra!
- Use cider-clojure-cli-aliases (#187) a2a7332 - Thanks @dakra!
- Fix typo (#184) 46eefd7 - Thanks @eval!

## 0.42.1 - 2023-06-22

### Bug Fixes

- Fix npm module resolution

## 0.42.0 - 2023-06-06

- Expose portal.ui.options to sci 5b88216
- Add :launcher :auto for using installed extensions 1d3f91d
- Allow any object to be invoked for rpc b5e422c
- Add zenburn theme 0b6d901

### Bug Fixes

- Fix sci code loading in vs-code extension b5e422c

## 0.41.0 - 2023-05-30

- Enable sci for calva notebooks 9cc28f1
- Improve reagent sci bindings 26188ae
- Improve word highlighting 6f58917
- Add clear-filter command to search bar 6ddb05e
- Call goto on link click for log viewer d6e24fc
- Add `portal.api/stop` 49a7923

### Bug Fixes

- Fix CLR reflection warnings 0e2134b
- Only focus value if window is focused a86fb81

## 0.40.0 - 2023-04-18

- Allow vscode extension to be used remotely

## 0.39.0 - 2023-04-16

- Update bb invocation and portal version number (#176) 8cd2422
  - Thanks @mathisto!
- Add print-err-fn to sci b026c67

### UX Tweaks

- Add toggle-expand to UI 3e393f6

### Bug Fixes

- Fix bug for evicting atoms from watch registry 3ac9d35
- Serialize `dispatch!` for async operations 5c34db0
- Fix reflection warning f3a6656
- Fix reflection warning e0e366a

### CLR Support

- Fix CLR browse fn on windows a2a3d81
- Fix CLR resource loading on windows 33a1490
- Fix CLR json-buffer reflection waring 54be295

## 0.38.2 - 2023-04-02

- Unmount notebook output item on dispose 0764a59
- Improve notebook repl error handling fcb741e
- Special print for remote values in UI 5181411
- Add cson support for remote api (#175) df7ed37 Thanks @Cyrik!

## 0.38.1 - 2023-03-30

- Fix advanced compile issue for calva notebooks

## 0.38.0 - 2023-03-30

- Add `portal.api/inspect` e1ecae5
- Updates IJ plugin for 2023.1 (#174) fd79e75
- [docs] update cider variable to include :portal alias (#172) 6ad7824
- Add `portal.viewer/pprint` limits 1cf2f48
- Add `js/Math` to sci be73bf4

### Calva Notebooks

- Improve error handling for notebooks 9bb4acd
- Improve notebook integration with nrepl middleware b24ceb6

## 0.37.1 - 2023-03-15

- Fix pom.xml generation ca290e2
- Removed unused code 692c86d
- Update prepl viewer to render return values daa55cd

## 0.37.0 - 2023-03-13

- Change shadow config to existing preload and change custom preloads away from
  portal namespace (#167) 7d27db1 Thanks @grahamcarlyle!

### NPM Support

- Add plotly viewer as an example da0439d
- Simplify node module loading code d0a0588
- Update sci to use `/load` endpoint 2e5437e
- Add `/load` endpoint to jvm server 57a0254
- First cut of `portal.runtime.npm/node-resolve` 70ce1a2

### Bug Fixes

- Enable async print in sci compiled fns 970ba8c
- Fix cljdoc build 07e6695
- Fix bug in goto-definition cbf10dd

## 0.36.0 - 2023-02-27

- Allow passing options to `portal.shadow.remote` d70730d

### CLR Support

- Add :cljr runtime logo 2d369c7
- Optionally load `clr.data.json` 52bb1d3
- Add basic CLR example project 49a9aa2
- Update `examples.data` for CLR 4aea238
- Port `portal.console` to CLR 9069afc
- First cut of ClojureCLR runtime support 8554ea2

### UX Tweaks

- Always use `:portal.viewer/inspector` for tap-list 729c8b7
- Improve exception viewer styles c806858

### Bug Fixes

- Fix :portal.viewer/test-report edge case 2fc888a
- Fix AM / PM bug in `:portal.viewer/date-time` 1979e36
- Fix tasks.build/prep hang 366dfd5
- Fix portal.api/close race condition on jvm 5addb24
- Fix test-report key for :type f5a7062
- Fix read-string for tagged-literals with metadata 4b99c87

## 0.35.1 - 2023-01-06

- Use pprint for collection of scalars d706d15
- Update how empty cells are styled d45ac1f
- Specialize `:portal.viewer/table` for maps bb43ab4
- Add :window-title to `portal.api/docs` f659c2e
- Fix typos. (#163) 64ce611 Thanks @maacl!
- Add ovsx publish (fixes #158) 509a606
- Remember ViewColumn between opens (#161) 2bbedc5 Thanks @PEZ!
- Fix broken badge for VS Marketplace (#159) fa73b69 Thanks @bpringe!

## 0.35.0 - 2022-11-30

- Updates IJ plugin for 2022.3 (#154) 4c71251
  - Thanks @coyotesqrl for the updates!
- Add portal.ui.select to sci libs aa99f9c
- Improve symbol go-to resolution 01f6942
- Add jwt viewer to quickly parse jwt strings 981ded5

### UX Tweaks

- Auto close repl when portal session is closed 5a177b5
- Explicitly fail when nrepl middleware is missing for `portal.api/repl` 6cb4507
- Increase click area for history buttons c7ebf53
- Target "_blank" absolute links d7f692f
- Add github markdown style callouts 0c97f45

### Documentation

- Add custom viewer docs 118cd29
- Update nrepl docs f54a4b4

### Viewer Docs

- Add `portal.api/docs` to open docs in portal a11c6b3
- Add jwt viewer to docs fed6916
- Add more time examples 8e18c15
- Add error example cases 60b30d9
- Add relative time example to viewer docs 016c238
- Add spec for diff viewer 4d2818f
- Add diff and csv to viewer docs bb2e016

### Bug Fixes

- Preserve `:ns` in portal.nrepl/wrap-repl 3e5a69f
  - Fixes `portal.api/repl` for vs-code
- Fix bb spec warning 497dd39
- Switch vega / vega-lite to svg for vs-code c70a359
- Account for filtering when selecting viewer 4d560cf
  - Thanks @seancorfield for the bug report!
- Fixes different values rendering the same (#119) 4090fa1
  - Thanks @daslu for the bug report!
- Fix java long rendering 5944ec8

## 0.34.2 - 2022-11-11

- Fix edge case around commands and default viewers 624f0b8
  - Thanks @rschmukler for the bug report!

## 0.34.1 - 2022-11-11

- Fix stackoverflow with volatiles b9de7ff
  - Thanks @wilkerlucio for the bug report!

## 0.34.0 - 2022-11-11

### UX Tweaks

- Only highlight background on filtering 97d7930
  - Thanks @dangercoder for the feedback!
- Allow copy + paste of binary data via edn 64580da
- Improve code viewer scroll styles e29340c
- Enable relative selection for hiccup viewer 08d6ac5
- Diff support for test output e892a64
- Remove hard-coded font name (Monaco) from themes. (#146) dcbb185 @finalfantasia
- Add ability to change initial folding for some data structure in a tree viewer (#143) 19a5977 @pfeodrippe

### Documentation

- Add example for consuming μ/log logs in Portal (#151) 7eea29f @walterl
- Fix bad hyperlink in README (#150) 6adb5c6 @The-Alchemist
- Add portal.console docs 04e384e
- Add nREPL customization docs 7ef5e28
- Add nREPL lein docs afd4926
- datafy - example of using nav (#148) e4b0f34 @holyjak
- Add leiningen examples to README (#147) 8dab116

### Viewer Docs

- Initial viewer docs 556bef2
- Add relative selection to doc viewer 2e6d742
- Highlight full tree path in doc viewer ed6e634

### Bug Fixes

- Keep filter highlight color in sync with context 10b4612
- Fix serialization of ##NaN, ##Inf and ##-Inf 36acbe5
  - Thanks @rschmukler for the bug report!
- Support fully qualified symbols in tagged values d036b44
- Prevent hiccup css from escaping to children 9fff14f
- Fix pprint for tagged values 4eb1771
- Fix multi select issue caused by 6b34ed1 fe11541

## 0.33.0 - 2022-10-18

- Add portal.api/url to get session url (#144) 9a9fad4
- Allow users to disable :launcher on p/open 0dfe342

### nREPL

- Tag portal.nrepl results e15865c
  - The tag in metadata is `{:portal.nrepl/eval true}`
  - This will allow for easier dispatch in tap handlers
- Update nrepl docs 95e6655
  - Must explicitly eval `(add-tap portal.api/submit)`
  - This is a bit of a breaking change but should mostly go unnoticed
- Allow users to intercept portal.nrepl results b124cd6

### Documentation

- Calva Clojure notebook docs beb4149
- Add portal.console docs 3830ea0
- Clerk docs 9be7c9d
- Update eval-str doc string fa6e557

## 0.32.0 - 2022-10-15

### New Features

- Allow users to await promises when using eval-str 4d612f4
- Default to nord-light based on system preferences 938dbfa
- First cut of nREPL repl middleware e32f38a
- Extend table viewer to seq of vectors dc9309c
- Adds `Shift + Enter` shortcut to clojure.datafy/datafy (#140) e98c4dc
  - Thanks @pfeodrippe!

### Bug Fixes

- Add port to websocket connection url f12beba
  - Thanks @hiredman!
- Improve relative selection for multi-table 7c28ce3

## 0.31.0 - 2022-10-04

### UX Tweaks

- Allow relative selection to work with gaps 4e924bd
- Allow selecting children in the same column 5b418d6
- Exclude messages without :file in portal.nrepl db85221
- Improve filtering for text viewer 41cb0e7
- Switch to icon for clear button d9355cf
- Improve blockquote styles 3382a85
- Update nrepl to ignore :portal.api/ignore e54f623
- Remove language label for code viewer 151c80d
- Better hiccup table styles 871e5c8
- Simplify hex/rgb preview 92b8468
- Try subscript notation for collection preview 7da5855
- Add `portal.ui.commands/clear-filter` 875c9a5
- Add filter highlighting based on context f4866a8
- Propagate click to parent when tab is selected 388ce78
- Allow users to share data via standalone mode 92973b2
- Allow maps to provide viewer metadata for vals 832d8a7

### Data Support

- Improve js object support d69921c
- Improve js array support 7c16a88
- Improve jvm ratio support 4de7152
- Improve java char support 8414560
- Improve cljs long representation c25370a

### Performance

- Improve ui selection performance 6cb33ba
- Improve markdown parsing performance d522a7c
- Improve highlight responsiveness e272f87

### Experimental

- Experimental emacs launcher support 83a0b0f
- First cut of cljdoc viewer de8a6d7

### Bug Fixes

- Fix bug in element-visible? afd0df9
- Use white-space pre-wrap for inspect-string a34b660
- Fix exception viewer coloring for vscode 0f451b0
- Fix tree viewer coloring in vscode f3bc908
- Improve goto for vs-code remote extension c1ae4ca
- Use get-location to have a stable selection 9450523
- Better error handling for date-time viewer 56bb787
- Fix path padding for Safari cf37214

## 0.30.0 - 2022-09-07

### New Features

- Add notebook renderer for vscode e1485e5
- Port extension code to node 3119a24
- Attach :portal.viewer/default meta to command args 7872e0a
- Support reading vars from edn strings 311ff43

### Improvements

- CSON perf updates (#137) 8c2f312
- Improve printing for tagged values 28ee0d8
- Updates build process for IJ plugin (#133) 82210ad
  - Thanks @coyotesqrl!
- Add more chrome search paths for windows a95870e

### Bug Fixes

- Fix log / exception viewer context 7cdebfc
- Isolate watch-registry per `*session*` 20ebc93
  - This prevents issues related to multiple instances with different atoms
    clearing each other's watch-registry.

## 0.29.1 - 2022-07-26

- Fix portal.nrepl middleware order issue 3d4cb04

## 0.29.0 - 2022-07-26

- Bumps IJ compatibility to 222.* (#132) ecd9116 - Thanks @coyotesqrl!
- intellij: Use console font size (#130) b7b6922 - Thanks @rads!
- Account for Portal PWA and Chrome alt browsers 0be369d
- Tidy up log viewer 589de9b
- Include stdio / test-report output in portal.nrepl 272b0fe
- Include eval time for portal.nrepl middleware 007c216
- Submit values in portal.nrepl when portal is open d86d8b3
- Support shadow-cljs evals for nrepl middleware e29bb48

## 0.28.0 - 2022-06-28

- Add improved shadow-cljs integration e0af8af
- Render caught command exceptions 952acc0
- Add clojure `clojure.spec.alpha/exercise` as a command 3ffbae8
- Switch to connections for more accurate session listing 9be02dc
- Try parsing objects as edn before rendering 9e1ab59

### Docs

- Update emacs elisp example to def portal to dev namespace (#129) d6886fc
  - Thanks @mschmele!
- Add nREPL guide 3458626
- Update shadow-cljs guide with new hook example fb8677e

## 0.27.0 - 2022-06-21

- Add portal.api/sessions to get current sessions bb390bb
  - Thanks for the feature idea @andyhorng!
- Adding diagnosis/troubleshooting as per Clojurians Slack discussion (#127) cecd2c9
  - Thanks for the improvement @realgenekim!
- Select value on double click nav eadfa84
- Improve exception rendering for json/transit/edn 580bcbd
- Allow users to proxy Portal behind a load balancer 37d2a88

### Bug Fixes

- Preserve sort order for sorted-maps / sorted-sets b0b962a
  - Thanks for the bug report @bortexz!
- Allow users to close drag and drop ui when stuck b0ecdc3
  - Thanks for the bug report @wilkerlucio!

## 0.26.0 - 2022-05-14

- Add shift h / shift l for history commands 1ae20b0
- Add space as shortcut for toggle-expand cbc7b7f
- Add relative viewer selection 6086262
  - Add `portal.ui.commands/select-prev-viewer`
  - Add `portal.ui.commands/select-next-viewer`
- Support instance api functions ad7062e
- Initial http viewer 238cf06
- Update nrepl middleware 2df0fdf
- Expand / collapse code viewer 3954914
- Expand / collapse for bin viewer f8f1e56
- Expand / collapse text viewer ef03645

### Bug Fixes

- Select root value on history push 350f8ce
- intellij: Improve initial options loading 471207f
- vs-code: Prevent app drag region e625065
- Fix clear race condition client error c7432e0

## 0.25.0 - 2022-04-27

- Allow `:runtime` to be specified for exceptions c3fdb55
- No `:max-height` on prepl viewer when root value 35b7cd4
- Default expand root values a2b300a
- Loosen up exception spec 9def199
- Improve test-report `:message` rendering 8bc77bd

### IntelliJ Extension Fixes

- Fix hidden portal window in IntelliJ on Linux. (#122) a316959
  - Thanks @Xceno!
- Update IntelliJ version compatibility range a432554

### Documentation

- Add exception guide 0958e90
- Test runner docs 311e47e
- Update theme docs ec37c6d
- Auto datafy values doc tip 2daf1fb
- Custom tap list doc 84f9b98
- JavaScript promise docs 92f36b2
- Add timbre docs 0afc062

## 0.24.0 - 2022-04-22

### Extensions

- Remove `~/.portal/electron.edn` on process exit 05c6824
- Remove `.portal/intellij.edn` on process exit e23adfe
- Remove `.portal/vs-code.edn` on process exit 09db810
- IJ Plugin - Bumps until version to run through 2022 (#121) 0458d1c - Thanks @coyotesqrl!
- IJ Plugin - Implements the `DumbAware` marker interface so plugin window works
  during indexing (#120) fb5c767  - Thanks @coyotesqrl!
- Fix electron destroy edge case 8fe5b76

### Viewers

- Initial `test-report` viewer e59838a
- Initial cut of prepl viewer 50a31e3
  - Reverse scroll priority for prepl term view 9835f18
- Exception viewer improvements 25ae988
- Improve map grid layout, prioritize vals over keys 4fe4da0

### Improvements

- Support app mode in windows 026a8ea
- Capture selected-viewer on nav / focus e8cc2ff
- Select table viewer by default for csv viewer a460e75
- Support :clojure.error keys for goto-definition 75b4369
- Add `parse-selected` command e88f360
- Only load vega styles once at root 7841c45
- Add print writer for `js/BigInt` fea5327
- Search in src / test for files b4909f2
- Improve error reporting for extensions e822df3

## 0.23.0 - 2022-04-05

### Additions

- Add gruvbox color theme (#118) eae54ec
  - Thanks @meditans!
- Add `:deps/prep-lib` to deps.edn d02528a

### Improvements

- Preserve lazy-take in state to reduce re-rendering 0c2c1bf
- Preserve location info across taps f2c7010
- Preserve white space for object printing aa92c90
- Parse ansi color codes with anser 3a1a7f0

### Bug Fixes

- Fix default-expand edge case 334d10c
- Distinguish between collection types in cache fb1368f
- Fix exception viewer edge case 7e9ff87
- Include metadata when capturing values in cache 42c1ce9

## 0.22.1 - 2022-03-16

- Support tagged literals with edn paste eec9e2b
- Fix cljdoc build fb31246

## 0.22.0 - 2022-03-15

### Additions

#### Table Viewer

- Implement table viewer for group-by maps 4086382

#### Log Viewer

- Expand / collapse log viewer 97da44a
  - Add portal as a runtime b5f77ba
- Use runtime logo in log viewer a425920

#### Commands

- Add `toggle-shell` command 8e2e4d8
- Add `select-pop` to account for multi-select 14ed32d
- Add `select-root` command 740ff1d
- Improve goto-definition to use :ns from log maps 499a0ef
- Select root when no value is currently selected 83365c8

#### Other

- Lighten up solarized-light border f104b5a
- Add vim style `:` shortcut for command prompt d787582
- Support unix timestamps 946cf34
- Add `j` and `k` to the selector-component 6716a1b
- Align exception viewer with error viewer fc879da
- Allow portal/open to take both session and options d3a7010
- Include ChunkedCons for clj and not bb 140e97d

### Extensions

- First cut of electron extension 4b44698
  - Move electron window to current workspace on open 3538260

- Update Intellij plugin description d7e4d2e
  - Now published on [Intellij Marketplace](https://plugins.jetbrains.com/plugin/18467-portal-inspector)!

### Bug Fixes

- Fix :options for portal extensions 9865217
- Fix default options and browser reload 951e73d
- Fix CORS error around `portal.client.web` a6fa446
- Port CORS fix a6fa446 to node runtime server 706f451

## 0.21.2 - 2022-02-16

### Bug Fixes

- Fix `p/eval-str` for production builds 01b881f

## 0.21.1 - 2022-02-16

### Bug Fixes

- Fix `portal.web/eval-str` return value fb7ad35
- Fix vs-code workspace folder resolution f8b7ef6
- Fix json option in main.clj 435dfe1
- Prompt user for format when pasting a value e821ddb
  - Trying to guess the data format from the clipboard is error prone and the
    previous UX of selecting a viewer on a string is not obvious.
  - This solution feels like a happy medium between both approaches.

## 0.21.0 - 2022-02-15

### Additions

- Expose portal.ui.state to sci 5b08197
  - Allows users to script Portal UI interactions
- Improve `p/eval-str` behavior 0377ef8
  - Return eval result or at least something printable instead of always
    returning nil.
- Add pprint viewer & command (#109) 5b753a1
  - Thanks @BrianChevalier!
- Port portal commands 6aff87d
  - Rename `load-clipboard` to `paste` f07b4e4
    - Try to parse the string from the clipboard before pushing a value into
      portal.
  - Allow paste and open-file to be used in other portal environments aside
    from standalone.

### Bug Fixes

- Fix theme and history 2fd1af4
- Fix vs-code extension for windows (#108) bd56c71
  - Thanks for the bug report @kjothen!

## 0.20.1 - 2022-01-23

Fix /icon.svg issue 842c1c7 - Thanks @heypragyan!

## 0.20.0 - 2022-01-23

With this next release, you can now specify a `:value` for portal to open. In
the case of static data, portal behaves as it did previously. With atoms, it
will automatically deref and update the UI when the atom changes. This new
feature with the hiccup viewer make it very easy to throw together live
development dashboards.

Also, special thanks to @BrianChevalier for all the awesome contributions in
this release! 2e077da 58e2eb3 378ee89 1acb6f8

### New API Options

- Prefer unqualified keys for options 0ca85bb
- Allow users to specify :editor option 65baf7c
- Add :on-load option for p/open (#104) 378ee89
- Allow setting portal defaults (#105) 1acb6f8

### Sync Runtimes

- Port Remote API to node runtime 0141c6a
- Fix node 17 host issue 5cdba58
  - Thanks @helins for the bug report!
- Add `portal.web/register!` 42c0ae9

### New Features

- Allow users to customize root value 6f719cc 75e07a5
  - Empty atom on clear bcbee50
  - Enable deref viewer for other atoms 28a8985
- Add experimental deref viewer 0cad30d
  - Move deref viewer icon above children viewers ac1e45c
  - Add initial GC hooks e480cbc
- Use metadata to specify custom table columns (#103) 2e077da
- Add copy to json command (#102) 58e2eb3

### Intellij Extension

- Publish intellij plugin to Jetbrains Marketplace 0cba51b

## 0.19.2 - 2021-12-28

- Release to fix cljdoc

## 0.19.1 - 2021-12-27

### Additions

- Add `portal.ui.commands/select-none` d1b1807
- Toggle expand all selected values a6c692f
- Add select-first / select-last 60fcbe5
- Use highlight.js for syntax highlighting strings b26c6f1
- Add more libs to sci 1f45777

### UX Fixes

- Only scroll to value if not visible cd00b5e
- Fix tree viewer white space 719eaab
- Fix select-child for table viewer 7cdcd72

## 0.19.0 - 2021-12-16

### Portal UI Extensions

- Add `portal.api/eval-str` 3e7770e
- Include ui for clojure-lsp indexing and docs 7d55927
- Expose portal.ui.commands to sci plugins 2648ab4
- Expose npm deps directly to sci portal extensions fa5b8d1
- Allow users to register their own viewers ae46284
- Allow runtime to eval code on the client af901fb

Docs /examples coming soon!

## 0.18.2 - 2021-12-15

Prefer inspector to vega viewers d350374

Since implementing a correct and complete spec for vega/vega-lite is
difficult, the spec was loosened up. However, now data that is not vega
is being matched for the vega viewer. By preferring the inspector, the
vega spec can remain open while providing a more reasonable default for
most data.

## 0.18.1 - 2021-12-08

- Enable alt for multi-select a1dc433
- Update vega lite spec to be more open (#82) 37a1e18 @BrianChevalier
- Improve file detection for cljs in `portal.console` 59110dc @Cyrik
- Fixes warning about `random-uuid` in Clojure 1.11 Alpha 3 (#81) 62b74cb @seancorfield
- Optionally use `IntersectionObserver` in Portal UI b667c71

### Intellij Extension

- Manage multiple project instances on IntelliJ extension (#85) f8205fa @wilkerlucio
- Add IntelliJ instructions (#89) fd24ed4 @holyjak a710e9e @Lambeaux
- Bump Intellij extension version 7f43062

## 0.18.0 - 2021-11-09

### Bug Fixes

- Fix search input focus ddda7c5
- Fix tab-to-value 41bb66d

Thanks [@seancorfield](https://github.com/seancorfield) for the bug reports!

### Intellij Extension

- Use random port for intellij extension b0c407f
- Put options in session storage to avoid data race 427f5a1
  - Thanks [@wilkerlucio](https://github.com/wilkerlucio) for the bug report!
- Always enable tool window in intellij extension 3a4e835
  - Thanks [@markaddleman](https://github.com/markaddleman) for the suggestion!

### Experimental

- First cut of remote api 188eb04
 - feat: addition of node client d7476b1
 - feat: addition of web client for standalone mode 396506a
 - feat: allow CORS on the standalone server 1e279cc
 - feat: addition of jvm+bb client 1348297

Thanks [@davidpham87](https://github.com/davidpham87) for this new feature!

## 0.17.0 - 2021-11-07

### Highlights

- First cut of intellij extension 58ff20a
- First cut of multi-select ea9568b

Selecting multiple values allows for invoking n-arity functions.

The semantics will be:

```clojure
(apply f [first-selected second-selected ...])
```

### Improvements

- Tree viewer improvements f8e1195
  - Improve selection
    - Enable relative selection
    - Enable collection selection
  - Enable `portal.viewer/default`
- Add `TaggedLiteral` type support 0de75a5
- Allow relative selecting tagged literal values 2ac964f
- Add `copy-str` command fcbb1c9
- Add `merge` command 10e9625
- Add runtime type to `portal.console` b7cf00b
- Add rgb color support to string inspector b9af7ce
- Allow collapsing a value that is default expanded 3773a2f

### Bug Fixes

- Fix stale use-invoke 39ba128
- Focus filter command should respect history 929a18e
- Fix relative selection for `js/BigInt` 062c776
- Implement `IResolve` for hash maps 97a6840

## 0.16.3 - 2021-10-18

### Bug Fixes

- Switch to `data.json` for jvm serialization 6a25a70
  - Avoid [jackson](https://github.com/FasterXML/jackson) transitive dependency
  - Thanks [@arichiardi](https://github.com/arichiardi)!

### Themes

- Add initial port of zerodark color theme (#72) fea9668
  - Thanks [@burinc](https://github.com/burinc)!
    - Use latest version in the examples 73b5f86

## 0.16.2 - 2021-10-15

### Bug Fixes

- Special representation of java longs, fixes #69 4120fab
  - When the value of a java long falls out of the range of a javascript number,
    switch to a tagged string representation. The advantage of trying to keep
    numbers, numbers is for chart / graph like viewers.
  - Thanks [@esp1](https://github.com/esp1)!
- Selection should consider the current viewer e4d5ffd

## 0.16.1 - 2021-10-12

### Bug Fixes

- Scroll commands should respect history b385779
  - Prevent scroll-top / scroll-bottom from only working on the last rendered
    page.
- Allow command predicates to fail independently 61d8760
  - This would prevent other runtime functions from working in the command
    palette
- Fix clojure.datafy/nav `enter` shortcut 71f09a8

## 0.16.0 - 2021-10-03

- Add `portal.api/register!` 97dbeb2

### UX Changes

- Focus filter input on `/` dcbb084
  - Press `enter` to re-focus the currently selected value
- Display multiple shortcuts in command palette d73f5f3
- Improve portal.api docs c7c77a6 4a5012a
- Sort commands by name 2d971f2
- Slurp should now work with strings d9e3b68
- Add scroll-top/bottom commands 0cbe016
  - The new `g g` shortcut will scroll to the top
  - The new `shift+g` shortcut will scroll to the bottom
- Prefer selected text when copying a7dd105
- Add `portal.runtime.jvm.editor/goto-definition` c926846 8d51f39
  - It will try and resolve the currently selected value to a file/line/column
    location and open it in an editor
  - emacs and vs-code are the currently supported editors
  - Works on many types including vars/files/strings
  - The new `g d` shortcut will also trigger this fn

###  VS Code extension

- Initial vs-code extension implementation (#66) c16978e
  - Save connection info in .portal/ 79457d8
  - Leverage session info on file open abc8bfa
  - Retain state in vs-code extension 0fa40f7
  - Recursively find a .portal directory upwards 9231004
  - Allow setting window title in vs-code tab ce9a50d
  - Set vs-code extension icon ddc35eb

Big thanks to [@seancorfield](https://github.com/seancorfield) for helping test
the VS Code extension!

### Experimental

- Add portal.console ns 7dcf526
  - Produces maps that will work with `goto-definition`
- First cut of log viewer fcdbd24
  - Correlate log level with color 7eff3d3

## 0.15.1 - 2021-09-19

- Fix issue with resource files

## 0.15.0 - 2021-09-19

- Initial bin hex/ascii viewer for binary data

### UX Changes

- Only filter currently selected value
  - Improve filtering instructions
- Improve relative selection
  - Bring relative selection to the table viewer
  - Add hjkl vim style shortcuts
  - Allow selecting parents / children
- Disable lazy loading

### Experimental

- First cut of remote repl integration
  - Support for clj/planck over socket repl

## 0.14.0 - 2021-08-15

- Preserve scroll history
- Make :field optional in vega lite spec [#62](https://github.com/djblue/portal/pull/62)
  - Thanks [@rfhayashi](https://github.com/rfhayashi)!
- Client pushes selected value back to Portal Atom [#60](https://github.com/djblue/portal/pull/60)
  - This enables the portal atom for the node.js runtime.
  - Thanks [@andyhorng](https://github.com/andyhorng)!
- Add solution for multimethod precedence error [#63](https://github.com/djblue/portal/pull/63)
  - Thanks [@BrianChevalier](https://github.com/BrianChevalier)!
- Links open in new window for hiccup viewer.
- Treat records as simple maps in portal client, useful if you have a list of
  records and want to view them in a table.
- Add `clojure.core/dissoc` to command palette.

### Performance

- The runtime will now wait for a stable tap-list before pushing it to the
  client.
- Leverage
  [`reagent.core/track`](https://github.com/reagent-project/reagent/blob/master/doc/ManagingState.md#the-track-function)
  to improve performance of portal interactions with large collections.

## 0.13.0 - 2021-08-08

- Hide `portal.runtime` metadata
- Improve portal viewer performance
- Leverage connection level caching to improve serialization performance

## 0.12.0 - 2021-06-12

- Records can now be viewed as maps
- Remove incognito chrome flag for chrome app
- Improve table viewer
  - Distinguish header rows / columns from data
  - Highlight row / column as you hover over elements
  - Fix path for selected value
- Map `META` in shortcuts to `⌘` for mac users
- Add selected path with copy button to bottom of portal
- Add `:portal.viewer/relative-time` for instants
  - ex: `1 day ago`
- Add `:portal.viewer/date-time` for instants
  - ex: `6/12/2021 6:32:28 PM`
- Viewers that parse data have a tab to get back to the original value

## 0.11.2 - 2021-05-05

- Stop excluding transitive deps

## 0.11.1 - 2021-04-25

- Fix firefox issues
  - Fix command palette css issue
  - Add `ctrl+j` shortcut, `ctrl+shift+p` is already mapped to launch private
    window
- Add `:portal.launcher/app` option
  - Allows users to opt-out of the default standalone chrome app mode
    - NOTE: this is already the case when chrome isn't installed

## 0.11.0 - 2021-04-14

- Alert user when disconnected from host runtime

### Viewer Updates

- Preview generic objects like strings
  - Allow users to expand / collapse the pr-str version of the object with `e`
- Allow users to render viewers from hiccup (experimental)
  - Checkout the [pwa](https://djblue.github.io/portal/) hiccup example for a
    demo
- Sticky keys / values in inspector map view
  - Align keys / values at the top of a map entry pair instead of the middle
- Include the key in path if the key is selected when calling
  `portal.command/copy-path`
- Add bigint support

### Command Palette Updates

- Add placeholder help to command-palette
- Add tab to command palette shortcuts
  - This prevents tabbing / shift+tabbing focus of selected elements when
    command palette is open.
- Put docs in command palette when an item is active
- Add select-viewer command

### Implementation Updates

- Switch to custom serialization format `cson`
  - Fixes transit error when encoding symbol keys with metadata
    [#11](https://github.com/djblue/portal/issues/11)
  - Fixes issues with older version of transit-clj (0.8.309) brought in with
    ClojureScript

## 0.10.0 - 2021-03-15

- Add command button to improve discoverability (bottom right)
- Expand items without nav-ing into them
  - Allow string expansion
  - Middle click to expand items
- Add preview type to each collection as header

### Item Selection

- Add the concept of a selection
  - Single click to select an item
  - Double click to select + nav into an item
  - Use arrow keys to move selection around
    - Limited to `:portal.viewers/inspector` for now
  - Change the viewer for a selected item

### Vega Viewer

- Initial Vega Viewer
  - Added by [@BrianChevalier](https://github.com/BrianChevalier)
  - In PR [#44](https://github.com/djblue/portal/pull/44)
  - Thanks!

### Material UI Theme

- Added material-ui theme
  - Added by [@rzwiefel](https://github.com/rzwiefel)
  - In PR [#45](https://github.com/djblue/portal/pull/45)
    - Also fixed hex color regex
  - Thanks!

### Window Title

- Adds an option to set a custom window title when opening portal
  - Added by [@coyotesqrl](https://github.com/coyotesqrl)
  - In PR [#42](https://github.com/djblue/portal/pull/42)
  - Thanks!

## 0.9.0 - 2021-01-22

- PWA startup for linux with >1 chrome profile (thanks [@brdloush](https://github.com/brdloush) [#41](https://github.com/djblue/portal/pull/41))
- Adds portal.api/submit target function for add-tap and remove-tap. (thanks [@coyotesqrl](https://github.com/coyotesqrl) [#39](https://github.com/djblue/portal/pull/39))
  - Now users can `(clojure.core/remove-tap #'portal.api/submit)` to stop sending `tap>`  values to portal.
- jvm.launcher: use system prop for path separator (thanks [@bennyandresen](https://github.com/bennyandresen) [#34](https://github.com/djblue/portal/pull/34))
  - fallback to previous value ":"
  - This improves Windows support. Unix uses ":" and Windows uses ";"

## 0.8.0 - 2021-01-06

- Support portal parameter in portal.api/open (thanks [@rfhayashi](https://github.com/rfhayashi) [#32](https://github.com/djblue/portal/pull/32))
  - This makes it possible to "reopen" a closed portal session
- Initial vega-lite & portal charts viewers (thanks [@BrianChevalier](https://github.com/BrianChevalier) [#31](https://github.com/djblue/portal/pull/31))
- Remove shift+c shortcut
- Improve hiccup styles
- Improve copy-as-edn by improving printing
  - Add *print-length* and *print-level*
- Add ratio support (thanks [@pangloss](https://github.com/pangloss) [#28](https://github.com/djblue/portal/issues/28))
- Initial cut of reverse search for commands
  - ctrl + r - Allows for executing previous command
- Fix filter-data for infinite seqs

## 0.7.0 - 2020-12-12

- Bump min babashka version from 0.2.2 to 0.2.4

### New

- Add YAML support (thanks [@yvendruscolo](https://github.com/yvendruscolo) [#27](https://github.com/djblue/portal/pull/27))
- Add a default viewer via the metadata key `:portal.viewer/default` (thanks [@JJ-Atkinson](https://github.com/JJ-Atkinson) [#24](https://github.com/djblue/portal/pull/24))
- Add more data manipulation commands
  - type, deref, datafy, bean
  - slurp for files, URL and URI
  - first, rest, count
  - get, get-in
- Update scrollbar style

### Fixed

- Prevent weird scrolling issue on osx
- Fix command palette css for safari
- Close portal on process shutdown for portal.main
- Find chrome binary from within wsl

### Changed

- Remove auto-datafy of objects pulled by portal
  - You can now run datafy manually via the command palette.

## 0.6.4 - 2020-11-15

- Chunk seqs into groups of 100 for serialization
  - Thanks [@l0st3d](https://github.com/l0st3d) [#17](https://github.com/djblue/portal/issues/17) for the bug report!

## 0.6.3 - 2020-11-02

- Print url when open fails (thanks, [@djomphe-elyada](https://github.com/djomphe-elyada) [#21](https://github.com/djblue/portal/issues/21))

## 0.6.2 - 2020-10-26

- Try to sort maps before rendering
- Switch viewer from command palette
- Add options for specifying port and host of launched UI (thanks, [@justone](https://github.com/justone) [#19](https://github.com/djblue/portal/issues/19))

## 0.6.1 - 2020-10-12

- Ensure active element is always visible
- Add data transformation commands
  - keys
  - vals
  - select-keys
  - select-columns
  - transpose-map
- Add new enumerated selection input

## 0.6.0 - 2020-10-11

- Initial release of command palette and shortcuts
  - `cmd+shift+p` on osx and `ctrl+shift+p` everywhere else
  - Other shortcuts are listed in the command palette next to the command
- Add edn, json, transit and csv viewers
- Only pull the last 25 tap items
- Include version and runtime type in app title
- Switch rpc to web-sockets
  - Switch to http-kit for jvm and bb runtimes
    - Require a minimum version of 0.2.2 for babashka
  - Bundle `ws` for node clojurescript runtime
- Fix nav issue with diff viewer

## 0.5.1 - 2020-09-16

- Fix portal.main

## 0.5.0 - 2020-09-10

- Remove accidentally included example data in release jar
- Table viewer updates
  - prevent text wrapping
  - fix coll of map indexing
  - sticky row and column headers
  - reduce padding to increase info density
- Allow window refresh and zoom in / out for portal.web
- Add pseudo portal atom

For jvm and web:

- `portal.api/open` will return an atom like thing
  - Use `deref` to get the current value viewed in portal
  - Use `reset!` to push a value into portal's history
  - Use `swap!` to apply a fn to the current value then `reset!` the
    result

For node and bb:

- `portal.api/open` will continue to return nil
  - bb support will be added when interfaces can be implemented
  - node support is hard since it can't block synchronously

## 0.4.1 - 2020-08-31

- Fix windows issues (thanks, [@MrGung](https://github.com/MrGung) [#12](https://github.com/djblue/portal/issues/12))
  - Explicitly set string encoding for Windows
- Fix markdown viewer code style
- Fix support for binary data
  - Stop excluding commons-codec/commons-codec

## 0.4.0 - 2020-08-17

- Add support for next release of babashka
  - Requires changes only presently available in master
  - Switch to cheshire for json
- Table viewer updates for map of maps (thanks, [@BrianChevalier](https://github.com/BrianChevalier) [#9](https://github.com/djblue/portal/pull/9))
- Slim down dependencies
- Expose theme setting via `portal.api/open`

## 0.3.1 - 2020-08-07

- Prevent re-initialization of web portal on page reload
- Fix portal on node when it can't find chrome
- Explicitly return nil from api functions

## 0.3.0 - 2020-08-05

- First cut of `portal.web`, the browser specific api
  - Mirrors portal.api for jvm and node
  - Exported for use in js dev console as well
    - `portal.web.open()`
    - `portal.web.close()`
    - `portal.web.clear()`
    - `portal.web.tap()`
- Fix nav to nil exception (thanks, [@rzwiefel](https://github.com/rzwiefel) [#8](https://github.com/djblue/portal/pull/8))
- Spawn chrome app in future to prevent blocking
- Initial keyboard support
  - Tab to move focus to next nav target
  - Confirm to nav into currently focused item
- Add lazy rendering to map, coll and table viewer

## 0.2.1 - 2020-08-02

- Fix issue with finding chrome in windows
- Fix issue with transit, tagged values and metadata

## 0.2.0 - 2020-08-02

- Add drag and drop feature to portal
- Add lines numbers to text viewer
- Filter lines in text viewer
- Lazy render text viewer
- Add forward navigation
- Preserve viewer selection on navigation
- Preserve filter text on navigation
- Disable navigation buttons when not possible

## 0.1.1 - 2020-07-26

- Special case rendering urls encoded as strings
- Simplify filtering
- Improve tree viewer complex key handling

## 0.1.0 - 2020-07-24

- Initial release

