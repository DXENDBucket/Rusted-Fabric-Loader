using Microsoft.Win32;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Web.Script.Serialization;
using System.Windows.Forms;

namespace RustedFabricInstaller
{
    internal static class Program
    {
        [STAThread]
        private static int Main(string[] args)
        {
            if (args.Any(delegate(string value) {
                return string.Equals(value, "--verify-payload", StringComparison.OrdinalIgnoreCase);
            }))
            {
                try
                {
                    Payload.Verify();
                    return 0;
                }
                catch
                {
                    return 2;
                }
            }

            if (args.Length == 2
                && string.Equals(args[0], "--install-test", StringComparison.OrdinalIgnoreCase))
            {
                try
                {
                    InstallerEngine.Install(new InstallOptions {
                        GameDirectory = args[1],
                        InstallApi = true,
                        InstallModMenu = true,
                        InstallIniEssentials = false,
                        InstallPerformanceProfiler = true,
                        CreateShortcut = false
                    }, delegate(string ignored) { });
                    return 0;
                }
                catch
                {
                    return 3;
                }
            }

            Application.EnableVisualStyles();
            Application.SetCompatibleTextRenderingDefault(false);
            Application.Run(new InstallerForm());
            return 0;
        }
    }

    internal sealed class InstallerForm : Form
    {
        private readonly TextBox gameDirectory = new TextBox();
        private readonly Button browse = new Button();
        private readonly CheckBox api = new CheckBox();
        private readonly CheckBox modMenu = new CheckBox();
        private readonly CheckBox iniEssentials = new CheckBox();
        private readonly CheckBox performanceProfiler = new CheckBox();
        private readonly CheckBox shortcut = new CheckBox();
        private readonly CheckBox riskAcceptance = new CheckBox();
        private readonly LinkLabel disclaimerLink = new LinkLabel();
        private readonly Button install = new Button();
        private readonly ProgressBar progress = new ProgressBar();
        private readonly TextBox log = new TextBox();

        public InstallerForm()
        {
            Text = "Rusted Fabric Loader 安装器";
            try { Icon = Icon.ExtractAssociatedIcon(Application.ExecutablePath); }
            catch { }
            StartPosition = FormStartPosition.CenterScreen;
            ClientSize = new Size(690, 640);
            MinimumSize = new Size(650, 620);
            MaximizeBox = false;
            AutoScaleMode = AutoScaleMode.Dpi;
            Font = new Font("Microsoft YaHei UI", 9F, FontStyle.Regular, GraphicsUnit.Point);

            Label title = new Label();
            title.Text = "Rusted Fabric Loader";
            title.Font = new Font(Font.FontFamily, 18F, FontStyle.Bold);
            title.AutoSize = true;
            title.Location = new Point(24, 20);
            Controls.Add(title);

            Label subtitle = new Label();
            subtitle.Text = "为现有 Rusted Warfare 安装 Fabric Loader（不包含游戏本体）";
            subtitle.AutoSize = true;
            subtitle.ForeColor = Color.DimGray;
            subtitle.Location = new Point(27, 58);
            Controls.Add(subtitle);

            Label pathLabel = new Label();
            pathLabel.Text = "游戏目录 / Game directory";
            pathLabel.AutoSize = true;
            pathLabel.Location = new Point(27, 94);
            Controls.Add(pathLabel);

            gameDirectory.Location = new Point(30, 116);
            gameDirectory.Size = new Size(540, 27);
            gameDirectory.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            Controls.Add(gameDirectory);

            browse.Text = "浏览…";
            browse.Location = new Point(580, 114);
            browse.Size = new Size(80, 30);
            browse.Anchor = AnchorStyles.Top | AnchorStyles.Right;
            browse.Click += BrowseClicked;
            Controls.Add(browse);

            GroupBox components = new GroupBox();
            components.Text = "安装组件 / Components";
            components.Location = new Point(30, 158);
            components.Size = new Size(630, 173);
            components.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            Controls.Add(components);

            api.Text = "Rusted Fabric API（推荐，大多数 Java 模组需要）";
            api.Checked = true;
            api.AutoSize = true;
            api.Location = new Point(18, 28);
            components.Controls.Add(api);

            modMenu.Text = "Java Mod Menu（推荐，游戏内查看 Java 模组）";
            modMenu.Checked = true;
            modMenu.AutoSize = true;
            modMenu.Location = new Point(18, 56);
            modMenu.CheckedChanged += ComponentDependencyChanged;
            components.Controls.Add(modMenu);

            iniEssentials.Text = "INI Essentials（可选 INI 扩展字段）";
            iniEssentials.Checked = false;
            iniEssentials.AutoSize = true;
            iniEssentials.Location = new Point(18, 84);
            components.Controls.Add(iniEssentials);

            performanceProfiler.Text = "Performance Profiler (optional, in-game diagnostics)";
            performanceProfiler.Checked = false;
            performanceProfiler.AutoSize = true;
            performanceProfiler.Location = new Point(18, 112);
            performanceProfiler.CheckedChanged += ComponentDependencyChanged;
            components.Controls.Add(performanceProfiler);

            shortcut.Text = "创建桌面快捷方式 / Create desktop shortcut";
            shortcut.Checked = true;
            shortcut.AutoSize = true;
            shortcut.Location = new Point(18, 140);
            components.Controls.Add(shortcut);
            ComponentDependencyChanged(null, EventArgs.Empty);

            GroupBox safety = new GroupBox();
            safety.Text = "安全与免责声明 / Security notice";
            safety.Location = new Point(30, 343);
            safety.Size = new Size(630, 125);
            safety.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            Controls.Add(safety);

            Label riskWarning = new Label();
            riskWarning.Text = "第三方 Java 模组是与游戏同权限运行的可执行代码，可能访问文件和网络、损坏存档或泄露隐私。\r\nThird-party Java mods are executable code and may access files/network, damage saves, or expose data.";
            riskWarning.ForeColor = Color.FromArgb(145, 72, 0);
            riskWarning.Location = new Point(16, 23);
            riskWarning.Size = new Size(598, 43);
            safety.Controls.Add(riskWarning);

            riskAcceptance.Text = "我已阅读并理解风险，只会加载我信任的 Java 模组 / I understand and accept the risk";
            riskAcceptance.Location = new Point(16, 67);
            riskAcceptance.Size = new Size(598, 24);
            riskAcceptance.CheckedChanged += delegate {
                install.Enabled = riskAcceptance.Checked;
            };
            safety.Controls.Add(riskAcceptance);

            disclaimerLink.Text = "查看完整免责声明 / View full disclaimer";
            disclaimerLink.AutoSize = true;
            disclaimerLink.Location = new Point(18, 95);
            disclaimerLink.LinkClicked += ShowDisclaimer;
            safety.Controls.Add(disclaimerLink);

            log.Location = new Point(30, 480);
            log.Size = new Size(630, 82);
            log.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            log.Multiline = true;
            log.ReadOnly = true;
            log.ScrollBars = ScrollBars.Vertical;
            log.BackColor = SystemColors.Window;
            Controls.Add(log);

            progress.Location = new Point(30, 575);
            progress.Size = new Size(480, 22);
            progress.Anchor = AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            Controls.Add(progress);

            install.Text = "安装或更新 / Install or update";
            install.Location = new Point(525, 570);
            install.Size = new Size(135, 34);
            install.Anchor = AnchorStyles.Bottom | AnchorStyles.Right;
            install.Enabled = false;
            install.Click += InstallClicked;
            Controls.Add(install);

            string detected = GameLocator.FindInstallation();
            if (!string.IsNullOrEmpty(detected))
            {
                gameDirectory.Text = detected;
                AppendLog("已检测到游戏目录：" + detected);
            }
            else
            {
                AppendLog("请选择 Rusted Warfare 游戏目录。");
            }
            AppendLog("安装包版本：" + Payload.ReadVersion());
        }

        private void ComponentDependencyChanged(object sender, EventArgs args)
        {
            if (modMenu.Checked || performanceProfiler.Checked)
            {
                api.Checked = true;
                api.Enabled = false;
            }
            else
            {
                api.Enabled = true;
            }
        }

        private void BrowseClicked(object sender, EventArgs args)
        {
            try
            {
                string selected = ExplorerFolderPicker.Show(
                    Handle, "选择 Rusted Warfare 游戏目录", gameDirectory.Text);
                if (!string.IsNullOrEmpty(selected)) gameDirectory.Text = selected;
            }
            catch (COMException)
            {
                BrowseWithLegacyDialog();
            }
            catch (EntryPointNotFoundException)
            {
                BrowseWithLegacyDialog();
            }
        }

        private void BrowseWithLegacyDialog()
        {
            using (FolderBrowserDialog dialog = new FolderBrowserDialog())
            {
                dialog.Description = "选择 Rusted Warfare 游戏目录";
                dialog.ShowNewFolderButton = false;
                if (Directory.Exists(gameDirectory.Text)) dialog.SelectedPath = gameDirectory.Text;
                if (dialog.ShowDialog(this) == DialogResult.OK)
                    gameDirectory.Text = dialog.SelectedPath;
            }
        }

        private async void InstallClicked(object sender, EventArgs args)
        {
            if (!riskAcceptance.Checked)
            {
                MessageBox.Show(this,
                    "请先阅读并确认第三方 Java 模组的代码执行风险。\r\n"
                    + "Read and accept the third-party Java mod code-execution risk first.",
                    "需要确认 / Confirmation required", MessageBoxButtons.OK,
                    MessageBoxIcon.Warning);
                return;
            }
            InstallOptions options = new InstallOptions();
            options.GameDirectory = gameDirectory.Text.Trim();
            options.InstallApi = api.Checked;
            options.InstallModMenu = modMenu.Checked;
            options.InstallIniEssentials = iniEssentials.Checked;
            options.InstallPerformanceProfiler = performanceProfiler.Checked;
            options.CreateShortcut = shortcut.Checked;

            SetBusy(true);
            try
            {
                InstallSummary summary = await Task.Run(delegate {
                    return InstallerEngine.Install(options, ReportFromWorker);
                });
                AppendLog("安装或更新完成，共写入 " + summary.FileCount + " 个文件。");
                DialogResult launch = MessageBox.Show(this,
                    "Rusted Fabric Loader 安装或更新完成。\r\n"
                    + "游戏本体、用户模组和设置均已保留。\r\n\r\n是否立即启动？",
                    "安装或更新完成", MessageBoxButtons.YesNo, MessageBoxIcon.Information);
                if (launch == DialogResult.Yes)
                {
                    Process.Start(new ProcessStartInfo {
                        FileName = Path.Combine(options.GameDirectory, "RustedFabricLauncher.exe"),
                        WorkingDirectory = options.GameDirectory,
                        UseShellExecute = true
                    });
                }
            }
            catch (Exception failure)
            {
                AppendLog("安装失败：" + failure.Message);
                MessageBox.Show(this, failure.Message, "安装失败 / Installation failed",
                    MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
            finally
            {
                SetBusy(false);
            }
        }

        private void ReportFromWorker(string message)
        {
            if (IsDisposed) return;
            BeginInvoke(new Action<string>(AppendLog), message);
        }

        private void ShowDisclaimer(object sender, LinkLabelLinkClickedEventArgs args)
        {
            using (Form dialog = new Form())
            {
                dialog.Text = "安全与第三方内容免责声明";
                dialog.StartPosition = FormStartPosition.CenterParent;
                dialog.ClientSize = new Size(720, 560);
                dialog.MinimumSize = new Size(600, 460);
                dialog.Font = Font;
                dialog.Icon = Icon;

                TextBox text = new TextBox();
                text.Multiline = true;
                text.ReadOnly = true;
                text.ScrollBars = ScrollBars.Vertical;
                text.WordWrap = true;
                text.BackColor = SystemColors.Window;
                text.Text = Payload.ReadDisclaimer();
                text.Location = new Point(16, 16);
                text.Size = new Size(688, 484);
                text.Anchor = AnchorStyles.Top | AnchorStyles.Bottom
                    | AnchorStyles.Left | AnchorStyles.Right;
                dialog.Controls.Add(text);

                Button close = new Button();
                close.Text = "关闭 / Close";
                close.DialogResult = DialogResult.OK;
                close.Location = new Point(574, 513);
                close.Size = new Size(130, 32);
                close.Anchor = AnchorStyles.Bottom | AnchorStyles.Right;
                dialog.Controls.Add(close);
                dialog.AcceptButton = close;
                dialog.CancelButton = close;
                dialog.ShowDialog(this);
            }
        }

        private void AppendLog(string message)
        {
            log.AppendText(message + Environment.NewLine);
        }

        private void SetBusy(bool busy)
        {
            install.Enabled = !busy && riskAcceptance.Checked;
            browse.Enabled = !busy;
            gameDirectory.Enabled = !busy;
            modMenu.Enabled = !busy;
            iniEssentials.Enabled = !busy;
            shortcut.Enabled = !busy;
            riskAcceptance.Enabled = !busy;
            disclaimerLink.Enabled = !busy;
            api.Enabled = !busy && !modMenu.Checked;
            progress.Style = busy ? ProgressBarStyle.Marquee : ProgressBarStyle.Blocks;
        }
    }

    internal static class ExplorerFolderPicker
    {
        private const uint FosPickFolders = 0x00000020;
        private const uint FosForceFileSystem = 0x00000040;
        private const uint FosPathMustExist = 0x00000800;
        private const uint FosDontAddToRecent = 0x02000000;
        private const uint SigdnFileSystemPath = 0x80058000;
        private const int CancelledHresult = unchecked((int)0x800704C7);

        public static string Show(IntPtr owner, string title, string initialPath)
        {
            IFileDialog dialog = (IFileDialog)new FileOpenDialogCom();
            try
            {
                uint existingOptions;
                dialog.GetOptions(out existingOptions);
                dialog.SetOptions(existingOptions | FosPickFolders | FosForceFileSystem
                    | FosPathMustExist | FosDontAddToRecent);
                dialog.SetTitle(title);
                dialog.SetOkButtonLabel("选择文件夹");

                if (Directory.Exists(initialPath))
                {
                    IShellItem initialFolder = CreateShellItem(initialPath);
                    try { dialog.SetFolder(initialFolder); }
                    finally { Marshal.FinalReleaseComObject(initialFolder); }
                }

                int result = dialog.Show(owner);
                if (result == CancelledHresult) return null;
                if (result < 0) Marshal.ThrowExceptionForHR(result);

                IShellItem selected;
                dialog.GetResult(out selected);
                try
                {
                    IntPtr value;
                    selected.GetDisplayName(SigdnFileSystemPath, out value);
                    try { return Marshal.PtrToStringUni(value); }
                    finally { Marshal.FreeCoTaskMem(value); }
                }
                finally { Marshal.FinalReleaseComObject(selected); }
            }
            finally
            {
                Marshal.FinalReleaseComObject(dialog);
            }
        }

        private static IShellItem CreateShellItem(string path)
        {
            Guid interfaceId = typeof(IShellItem).GUID;
            IShellItem item;
            SHCreateItemFromParsingName(path, IntPtr.Zero, ref interfaceId, out item);
            return item;
        }

        [DllImport("shell32.dll", CharSet = CharSet.Unicode, PreserveSig = false)]
        private static extern void SHCreateItemFromParsingName(
            string path, IntPtr bindingContext, ref Guid interfaceId,
            [MarshalAs(UnmanagedType.Interface)] out IShellItem item);

        [ComImport]
        [Guid("DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7")]
        private class FileOpenDialogCom
        {
        }

        [ComImport]
        [Guid("42F85136-DB7E-439C-85F1-E4075D135FC8")]
        [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IFileDialog
        {
            [PreserveSig]
            int Show(IntPtr parent);
            void SetFileTypes(uint count, IntPtr filterSpecifications);
            void SetFileTypeIndex(uint index);
            void GetFileTypeIndex(out uint index);
            void Advise(IntPtr events, out uint cookie);
            void Unadvise(uint cookie);
            void SetOptions(uint options);
            void GetOptions(out uint options);
            void SetDefaultFolder(IShellItem shellItem);
            void SetFolder(IShellItem shellItem);
            void GetFolder(out IShellItem shellItem);
            void GetCurrentSelection(out IShellItem shellItem);
            void SetFileName([MarshalAs(UnmanagedType.LPWStr)] string name);
            void GetFileName([MarshalAs(UnmanagedType.LPWStr)] out string name);
            void SetTitle([MarshalAs(UnmanagedType.LPWStr)] string title);
            void SetOkButtonLabel([MarshalAs(UnmanagedType.LPWStr)] string label);
            void SetFileNameLabel([MarshalAs(UnmanagedType.LPWStr)] string label);
            void GetResult(out IShellItem shellItem);
            void AddPlace(IShellItem shellItem, uint alignment);
            void SetDefaultExtension([MarshalAs(UnmanagedType.LPWStr)] string extension);
            void Close(int hresult);
            void SetClientGuid(ref Guid guid);
            void ClearClientData();
            void SetFilter(IntPtr filter);
        }

        [ComImport]
        [Guid("43826D1E-E718-42EE-BC55-A1E261C37BFE")]
        [InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
        private interface IShellItem
        {
            void BindToHandler(IntPtr bindingContext, ref Guid handlerId,
                ref Guid interfaceId, out IntPtr result);
            void GetParent(out IShellItem parent);
            void GetDisplayName(uint displayName, out IntPtr name);
            void GetAttributes(uint mask, out uint attributes);
            void Compare(IShellItem shellItem, uint hint, out int order);
        }
    }

    internal sealed class InstallOptions
    {
        public string GameDirectory;
        public bool InstallApi;
        public bool InstallModMenu;
        public bool InstallIniEssentials;
        public bool InstallPerformanceProfiler;
        public bool CreateShortcut;
    }

    internal sealed class InstallSummary
    {
        public int FileCount;
    }

    internal static class InstallerEngine
    {
        private const string ManifestRelativePath = "rusted-fabric-loader/install-manifest.json";

        public static InstallSummary Install(InstallOptions options, Action<string> log)
        {
            string game = ValidateGameDirectory(options.GameDirectory);
            if (options.InstallPerformanceProfiler && !options.InstallApi)
                throw new InvalidOperationException("Performance Profiler requires Rusted Fabric API.");
            if (options.InstallModMenu && !options.InstallApi)
                throw new InvalidOperationException("Java Mod Menu 依赖 Rusted Fabric API。");

            log("正在验证离线安装负载…");
            Payload.Verify();
            string staging = Path.Combine(game, "rusted-fabric-loader",
                ".install-staging-" + Guid.NewGuid().ToString("N"));
            string filesRoot = Path.Combine(staging, "files");
            string backupsRoot = Path.Combine(staging, "backups");
            Directory.CreateDirectory(filesRoot);

            List<StagedFile> staged = new List<StagedFile>();
            List<string> installedComponents = new List<string>();
            installedComponents.Add("loader");
            if (options.InstallApi) installedComponents.Add("api");
            if (options.InstallModMenu) installedComponents.Add("mod_menu");
            if (options.InstallIniEssentials) installedComponents.Add("ini_essentials");
            if (options.InstallPerformanceProfiler) installedComponents.Add("performance_profiler");

            try
            {
                using (ZipArchive archive = Payload.Open())
                {
                    foreach (ZipArchiveEntry entry in archive.Entries)
                    {
                        string component;
                        string relative;
                        if (!SelectEntry(entry.FullName, options, out component, out relative)) continue;
                        if (string.IsNullOrEmpty(relative) || entry.FullName.EndsWith("/")) continue;
                        relative = SafeRelativePath(relative);
                        string stagedPath = Path.Combine(filesRoot, relative);
                        Directory.CreateDirectory(Path.GetDirectoryName(stagedPath));
                        using (Stream input = entry.Open())
                        using (FileStream output = new FileStream(stagedPath, FileMode.CreateNew,
                            FileAccess.Write, FileShare.None))
                        {
                            input.CopyTo(output);
                        }
                        staged.Add(new StagedFile(component, relative, stagedPath, Sha256(stagedPath)));
                    }
                }

                string batRelative = "run-rusted-fabric.bat";
                string batPath = Path.Combine(filesRoot, batRelative);
                File.WriteAllText(batPath, BuildLaunchScript(), new UTF8Encoding(false));
                staged.Add(new StagedFile("loader", batRelative, batPath, Sha256(batPath)));

                if (!staged.Any(delegate(StagedFile file) {
                    return file.RelativePath.Equals("RustedFabricLauncher.exe", StringComparison.OrdinalIgnoreCase);
                })) throw new InvalidDataException("安装负载中缺少 RustedFabricLauncher.exe");

                log("正在写入 Loader 文件…");
                InstallManifest oldManifest = LoadManifest(Path.Combine(game,
                    ManifestRelativePath.Replace('/', Path.DirectorySeparatorChar)));
                CommitStagedFiles(game, staging, backupsRoot, staged);
                RemoveDeselectedManagedFiles(game, oldManifest, staged, log);
                CleanupLoaderOwnedDuplicates(game, staged, options, log);

                InstallManifest manifest = new InstallManifest();
                manifest.InstallerVersion = Payload.ReadVersion();
                manifest.InstalledAtUtc = DateTime.UtcNow.ToString("o");
                manifest.Components = installedComponents;
                manifest.Files = staged.Select(delegate(StagedFile file) {
                    return new ManagedFile {
                        Component = file.Component,
                        Path = file.RelativePath.Replace('\\', '/'),
                        Sha256 = file.Hash
                    };
                }).OrderBy(delegate(ManagedFile file) { return file.Path; }).ToList();
                WriteManifest(Path.Combine(game,
                    ManifestRelativePath.Replace('/', Path.DirectorySeparatorChar)), manifest);

                if (options.CreateShortcut)
                {
                    log("正在创建桌面快捷方式…");
                    ShortcutCreator.Create(game);
                }
                log("未修改游戏本体或第三方 javamods 文件。");
                return new InstallSummary { FileCount = staged.Count };
            }
            finally
            {
                TryDeleteDirectory(staging);
            }
        }

        private static string ValidateGameDirectory(string value)
        {
            if (string.IsNullOrWhiteSpace(value))
                throw new InvalidOperationException("请先选择 Rusted Warfare 游戏目录。");
            string game = Path.GetFullPath(value);
            if (!Directory.Exists(game))
                throw new DirectoryNotFoundException("游戏目录不存在：" + game);
            bool hasGame = File.Exists(Path.Combine(game, "game-lib.jar"))
                || File.Exists(Path.Combine(game, "libs", "game-lib.jar"));
            if (!hasGame)
                throw new FileNotFoundException("所选目录中找不到 game-lib.jar。\r\n安装器不包含游戏本体。");
            if (!File.Exists(Path.Combine(game, "jvm64", "bin", "java.exe")))
                throw new FileNotFoundException("所选目录中找不到 jvm64\\bin\\java.exe。");
            return game.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
        }

        private static bool SelectEntry(string entry, InstallOptions options,
                                        out string component, out string relative)
        {
            component = null;
            relative = null;
            if (entry.StartsWith("core/", StringComparison.Ordinal))
            {
                component = "loader";
                relative = entry.Substring("core/".Length);
                return true;
            }
            if (options.InstallApi && entry.StartsWith("components/api/", StringComparison.Ordinal))
            {
                component = "api";
                relative = entry.Substring("components/api/".Length);
                return true;
            }
            if (options.InstallModMenu && entry.StartsWith("components/mod_menu/", StringComparison.Ordinal))
            {
                component = "mod_menu";
                relative = entry.Substring("components/mod_menu/".Length);
                return true;
            }
            if (options.InstallIniEssentials
                && entry.StartsWith("components/ini_essentials/", StringComparison.Ordinal))
            {
                component = "ini_essentials";
                relative = entry.Substring("components/ini_essentials/".Length);
                return true;
            }
            if (options.InstallPerformanceProfiler
                && entry.StartsWith("components/performance_profiler/", StringComparison.Ordinal))
            {
                component = "performance_profiler";
                relative = entry.Substring("components/performance_profiler/".Length);
                return true;
            }
            return false;
        }

        private static string SafeRelativePath(string value)
        {
            string normalized = value.Replace('/', Path.DirectorySeparatorChar);
            if (Path.IsPathRooted(normalized)) throw new InvalidDataException("负载包含绝对路径");
            string[] parts = normalized.Split(Path.DirectorySeparatorChar);
            if (parts.Any(delegate(string part) { return part == ".." || part.Length == 0; }))
                throw new InvalidDataException("负载包含不安全路径：" + value);
            return string.Join(Path.DirectorySeparatorChar.ToString(), parts);
        }

        private static void CommitStagedFiles(string game, string staging, string backupsRoot,
                                              List<StagedFile> files)
        {
            List<StagedFile> committed = new List<StagedFile>();
            List<Tuple<string, string>> backups = new List<Tuple<string, string>>();
            try
            {
                foreach (StagedFile file in files)
                {
                    string destination = Path.Combine(game, file.RelativePath);
                    Directory.CreateDirectory(Path.GetDirectoryName(destination));
                    if (File.Exists(destination))
                    {
                        string backup = Path.Combine(backupsRoot, file.RelativePath);
                        Directory.CreateDirectory(Path.GetDirectoryName(backup));
                        File.Move(destination, backup);
                        backups.Add(Tuple.Create(destination, backup));
                    }
                    File.Move(file.StagedPath, destination);
                    committed.Add(file);
                }
            }
            catch
            {
                foreach (StagedFile file in committed.AsEnumerable().Reverse())
                {
                    string destination = Path.Combine(game, file.RelativePath);
                    if (File.Exists(destination)) File.Delete(destination);
                }
                foreach (Tuple<string, string> backup in backups.AsEnumerable().Reverse())
                {
                    if (File.Exists(backup.Item2))
                    {
                        Directory.CreateDirectory(Path.GetDirectoryName(backup.Item1));
                        File.Move(backup.Item2, backup.Item1);
                    }
                }
                throw;
            }
        }

        private static void RemoveDeselectedManagedFiles(string game, InstallManifest oldManifest,
                                                         List<StagedFile> current, Action<string> log)
        {
            if (oldManifest == null || oldManifest.Files == null) return;
            HashSet<string> retained = new HashSet<string>(
                current.Select(delegate(StagedFile file) { return file.RelativePath; }),
                StringComparer.OrdinalIgnoreCase);
            foreach (ManagedFile old in oldManifest.Files)
            {
                string relative;
                try { relative = SafeRelativePath(old.Path); }
                catch { continue; }
                if (retained.Contains(relative)) continue;
                string path = Path.Combine(game, relative);
                if (!File.Exists(path)) continue;
                if (!string.Equals(Sha256(path), old.Sha256, StringComparison.OrdinalIgnoreCase))
                {
                    log("保留已被修改的旧文件：" + relative);
                    continue;
                }
                File.Delete(path);
            }
        }

        private static void CleanupLoaderOwnedDuplicates(string game, List<StagedFile> current,
                                                         InstallOptions options, Action<string> log)
        {
            HashSet<string> retained = new HashSet<string>(current.Select(delegate(StagedFile file) {
                return Path.GetFullPath(Path.Combine(game, file.RelativePath));
            }), StringComparer.OrdinalIgnoreCase);
            CleanupPattern(Path.Combine(game, "fabric-libs"), "*.jar", retained, log);
            CleanupPattern(Path.Combine(game, "rusted-fabric-loader"),
                "rusted-fabric-loader-*.jar", retained, log);
            if (options.InstallApi)
                CleanupPattern(Path.Combine(game, "javamods"), "rusted-fabric-api-*.jar", retained, log);
            if (options.InstallModMenu)
                CleanupPattern(Path.Combine(game, "javamods"), "java-mod-menu-*.jar", retained, log);
            if (options.InstallIniEssentials)
                CleanupPattern(Path.Combine(game, "javamods"), "ini-essentials-*.jar", retained, log);
            if (options.InstallPerformanceProfiler)
                CleanupPattern(Path.Combine(game, "javamods"), "performance-profiler-*.jar", retained, log);
        }

        private static void CleanupPattern(string directory, string pattern, HashSet<string> retained,
                                           Action<string> log)
        {
            if (!Directory.Exists(directory)) return;
            foreach (string file in Directory.GetFiles(directory, pattern))
            {
                string full = Path.GetFullPath(file);
                if (retained.Contains(full)) continue;
                try { File.Delete(full); }
                catch (Exception failure) { log("无法删除旧 Loader 文件 " + file + "：" + failure.Message); }
            }
        }

        private static string BuildLaunchScript()
        {
            return string.Join("\r\n", new[] {
                "@echo off",
                "setlocal",
                "cd /d \"%~dp0\"",
                "set \"JAVA_EXE=%CD%\\jvm64\\bin\\java.exe\"",
                "set \"GP_CP=\"",
                "for %%F in (\"%CD%\\rusted-fabric-loader\\rusted-fabric-loader-*.jar\") do set \"GP_CP=%%~fF\"",
                "if not exist \"%JAVA_EXE%\" (echo Missing jvm64\\bin\\java.exe & pause & exit /b 1)",
                "if not defined GP_CP (echo Missing Rusted Fabric Loader GameProvider & pause & exit /b 1)",
                "\"%JAVA_EXE%\" -Xmx4096M -Dfile.encoding=UTF-8 -cp \"%CD%\\fabric-libs/*;%GP_CP%\" net.fabricmc.loader.impl.launch.knot.KnotClient %*",
                "set \"EXIT_CODE=%ERRORLEVEL%\"",
                "if not \"%EXIT_CODE%\"==\"0\" pause",
                "exit /b %EXIT_CODE%",
                ""
            });
        }

        private static string Sha256(string path)
        {
            using (SHA256 hash = SHA256.Create())
            using (FileStream input = File.OpenRead(path))
            {
                return BitConverter.ToString(hash.ComputeHash(input)).Replace("-", "").ToLowerInvariant();
            }
        }

        private static InstallManifest LoadManifest(string path)
        {
            if (!File.Exists(path)) return null;
            try
            {
                JavaScriptSerializer serializer = new JavaScriptSerializer();
                return serializer.Deserialize<InstallManifest>(File.ReadAllText(path, Encoding.UTF8));
            }
            catch { return null; }
        }

        private static void WriteManifest(string path, InstallManifest manifest)
        {
            Directory.CreateDirectory(Path.GetDirectoryName(path));
            JavaScriptSerializer serializer = new JavaScriptSerializer();
            string temporary = path + ".tmp";
            File.WriteAllText(temporary, serializer.Serialize(manifest), new UTF8Encoding(false));
            if (File.Exists(path)) File.Delete(path);
            File.Move(temporary, path);
        }

        private static void TryDeleteDirectory(string path)
        {
            try { if (Directory.Exists(path)) Directory.Delete(path, true); }
            catch { }
        }

        private sealed class StagedFile
        {
            public readonly string Component;
            public readonly string RelativePath;
            public readonly string StagedPath;
            public readonly string Hash;
            public StagedFile(string component, string relativePath, string stagedPath, string hash)
            {
                Component = component;
                RelativePath = relativePath;
                StagedPath = stagedPath;
                Hash = hash;
            }
        }
    }

    public sealed class InstallManifest
    {
        public string InstallerVersion { get; set; }
        public string InstalledAtUtc { get; set; }
        public List<string> Components { get; set; }
        public List<ManagedFile> Files { get; set; }
    }

    public sealed class ManagedFile
    {
        public string Component { get; set; }
        public string Path { get; set; }
        public string Sha256 { get; set; }
    }

    internal static class Payload
    {
        private const string ResourceName = "RustedFabricInstaller.Payload.zip";

        public static ZipArchive Open()
        {
            Stream stream = Assembly.GetExecutingAssembly().GetManifestResourceStream(ResourceName);
            if (stream == null) throw new InvalidDataException("安装器内嵌负载不存在");
            return new ZipArchive(stream, ZipArchiveMode.Read, false);
        }

        public static string ReadVersion()
        {
            using (ZipArchive archive = Open())
            {
                ZipArchiveEntry entry = archive.GetEntry("metadata/version.txt");
                if (entry == null) return "unknown";
                using (StreamReader reader = new StreamReader(entry.Open(), Encoding.UTF8))
                    return reader.ReadToEnd().Trim();
            }
        }

        public static string ReadDisclaimer()
        {
            using (ZipArchive archive = Open())
            {
                ZipArchiveEntry entry = archive.GetEntry("core/DISCLAIMER.md");
                if (entry == null) return "Disclaimer is missing from the installer payload.";
                using (StreamReader reader = new StreamReader(entry.Open(), Encoding.UTF8))
                    return reader.ReadToEnd();
            }
        }

        public static void Verify()
        {
            using (ZipArchive archive = Open())
            {
                string[] names = archive.Entries.Select(delegate(ZipArchiveEntry entry) {
                    return entry.FullName;
                }).ToArray();
                Require(names, "core/RustedFabricLauncher.exe");
                Require(names, "core/DISCLAIMER.md");
                RequirePrefix(names, "core/fabric-libs/");
                RequirePrefix(names, "core/rusted-fabric-loader/rusted-fabric-loader-");
                RequirePrefix(names, "components/api/javamods/rusted-fabric-api-");
                RequirePrefix(names, "components/mod_menu/javamods/java-mod-menu-");
                RequirePrefix(names, "components/ini_essentials/javamods/ini-essentials-");
                RequirePrefix(names, "components/performance_profiler/javamods/performance-profiler-");
                Require(names, "metadata/version.txt");
                foreach (string name in names)
                {
                    string lower = name.ToLowerInvariant();
                    if (lower.EndsWith(".apk") || lower.EndsWith(".dex")
                        || lower.Contains("game-lib") || lower.Contains("example-mod")
                        || lower.Contains("rusted_fabric_example"))
                        throw new InvalidDataException("安装负载包含禁止文件：" + name);
                }
            }
        }

        private static void Require(string[] names, string expected)
        {
            if (!names.Contains(expected)) throw new InvalidDataException("安装负载缺少：" + expected);
        }

        private static void RequirePrefix(string[] names, string prefix)
        {
            if (!names.Any(delegate(string name) { return name.StartsWith(prefix, StringComparison.Ordinal); }))
                throw new InvalidDataException("安装负载缺少：" + prefix);
        }
    }

    internal static class ShortcutCreator
    {
        public static void Create(string gameDirectory)
        {
            string shortcutPath = Path.Combine(Environment.GetFolderPath(
                Environment.SpecialFolder.DesktopDirectory), "Rusted Fabric Loader.lnk");
            Type shellType = Type.GetTypeFromProgID("WScript.Shell");
            if (shellType == null) throw new InvalidOperationException("WScript.Shell 不可用");
            object shell = Activator.CreateInstance(shellType);
            object shortcut = shellType.InvokeMember("CreateShortcut", BindingFlags.InvokeMethod,
                null, shell, new object[] { shortcutPath });
            Type shortcutType = shortcut.GetType();
            string launcher = Path.Combine(gameDirectory, "RustedFabricLauncher.exe");
            shortcutType.InvokeMember("TargetPath", BindingFlags.SetProperty, null, shortcut,
                new object[] { launcher });
            shortcutType.InvokeMember("WorkingDirectory", BindingFlags.SetProperty, null, shortcut,
                new object[] { gameDirectory });
            shortcutType.InvokeMember("Description", BindingFlags.SetProperty, null, shortcut,
                new object[] { "Launch Rusted Warfare with Rusted Fabric Loader" });
            shortcutType.InvokeMember("IconLocation", BindingFlags.SetProperty, null, shortcut,
                new object[] { launcher + ",0" });
            shortcutType.InvokeMember("Save", BindingFlags.InvokeMethod, null, shortcut, null);
        }
    }

    internal static class GameLocator
    {
        public static string FindInstallation()
        {
            foreach (string root in SteamRoots())
            {
                string candidate = Path.Combine(root, "steamapps", "common", "Rusted Warfare");
                if (IsGameDirectory(candidate)) return candidate;
                string vdf = Path.Combine(root, "steamapps", "libraryfolders.vdf");
                if (!File.Exists(vdf)) continue;
                try
                {
                    string text = File.ReadAllText(vdf);
                    MatchCollection paths = Regex.Matches(text, "\\\"path\\\"\\s+\\\"([^\\\"]+)\\\"");
                    foreach (Match path in paths)
                    {
                        string library = path.Groups[1].Value.Replace("\\\\", "\\");
                        candidate = Path.Combine(library, "steamapps", "common", "Rusted Warfare");
                        if (IsGameDirectory(candidate)) return candidate;
                    }
                }
                catch { }
            }

            foreach (DriveInfo drive in DriveInfo.GetDrives())
            {
                if (!drive.IsReady) continue;
                string[] relativeCandidates = {
                    "SteamLibrary\\steamapps\\common\\Rusted Warfare",
                    "Steam\\steamapps\\common\\Rusted Warfare"
                };
                foreach (string relative in relativeCandidates)
                {
                    string candidate = Path.Combine(drive.RootDirectory.FullName, relative);
                    if (IsGameDirectory(candidate)) return candidate;
                }
            }
            return null;
        }

        private static IEnumerable<string> SteamRoots()
        {
            HashSet<string> roots = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
            string programFiles = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86);
            if (!string.IsNullOrEmpty(programFiles)) roots.Add(Path.Combine(programFiles, "Steam"));
            try
            {
                using (RegistryKey key = Registry.CurrentUser.OpenSubKey("Software\\Valve\\Steam"))
                {
                    object value = key != null ? key.GetValue("SteamPath") : null;
                    if (value != null) roots.Add(value.ToString().Replace('/', '\\'));
                }
            }
            catch { }
            return roots;
        }

        private static bool IsGameDirectory(string path)
        {
            return Directory.Exists(path)
                && (File.Exists(Path.Combine(path, "game-lib.jar"))
                    || File.Exists(Path.Combine(path, "libs", "game-lib.jar")))
                && File.Exists(Path.Combine(path, "jvm64", "bin", "java.exe"));
        }
    }
}
