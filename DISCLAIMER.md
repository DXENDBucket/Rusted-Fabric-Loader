# Security and third-party content disclaimer / 安全与第三方内容免责声明

## 中文

Rusted Fabric Loader 会在游戏进程中加载 Java 模组。Java 模组是可执行代码，并不是只含
图片、地图或 INI 数据的普通资源包。模组运行时通常与游戏及 Loader 具有相同的操作系统
权限；恶意或存在缺陷的模组可能读取、修改或删除该账户或应用能够访问的文件，访问网络，
泄露隐私，损坏存档或游戏安装，造成崩溃、作弊或联机不兼容，甚至进一步危害设备安全。
Android 的应用沙箱只能限制应用整体的权限，不能在同一应用进程内隔离 Java 模组。

- 只安装你信任的来源提供的 Java 模组，优先选择能够检查源代码和校验值的发布渠道。
- 安装前备份存档、模组及其他重要文件。杀毒软件或 Loader 的格式检查不能保证模组安全。
- Rusted Fabric 项目不会自动审核、担保或认可任何第三方模组。名称中含有“Fabric”、
  “Rusted Fabric”或相似字样并不代表它是官方内容。
- 只有由本仓库及其 GitHub Release 直接发布、并能与公布校验值对应的 Loader、
  Rusted Fabric API、Java Mod Menu 和 INI Essentials 属于本项目官方发行物。重新打包、
  镜像或修改版本应由其分发者负责。
- 使用者应自行判断模组的合法性、联机规则与游戏服务条款，并承担加载第三方代码所产生的
  风险。在适用法律和项目许可证允许的最大范围内，本项目贡献者不对第三方内容造成的损失
  承担责任。

本项目不包含或分发 Rusted Warfare 游戏本体，亦不隶属于或代表游戏开发者。游戏名称及
相关商标归各自权利人所有；使用者必须自行合法取得游戏文件。

## English

Rusted Fabric Loader loads Java mods into the game process. A Java mod is executable code, not a
passive pack of images, maps, or INI data. It normally runs with the same operating-system
permissions as the game and Loader. A malicious or defective mod may read, modify, or delete files
available to that account or application, access the network, expose private information, corrupt
saves or installations, cause crashes, cheating, or multiplayer incompatibility, and otherwise
compromise the device. Android's application sandbox limits the application as a whole; it does not
isolate Java mods running inside the same application process.

- Install Java mods only from sources you trust. Prefer releases whose source and checksums can be
  inspected.
- Back up saves, mods, and other important files first. Antivirus software and Loader format checks
  cannot guarantee that a mod is safe.
- The Rusted Fabric project does not automatically review, warrant, or endorse third-party mods.
  A name containing “Fabric”, “Rusted Fabric”, or similar wording does not make a mod official.
- Only Loader, Rusted Fabric API, Java Mod Menu, and INI Essentials artifacts published directly by
  this repository and its GitHub Releases, matching the published checksums, are official project
  distributions. Repacked, mirrored, or modified builds are the distributor's responsibility.
- Users are responsible for legality, multiplayer rules, game terms, and the risks of loading
  third-party code. To the maximum extent permitted by applicable law and the project licenses,
  project contributors are not liable for damage caused by third-party content.

This project does not contain or distribute the Rusted Warfare game and is not affiliated with or
acting on behalf of its developer. Game names and related trademarks belong to their respective
owners. Users must obtain their game files lawfully.
