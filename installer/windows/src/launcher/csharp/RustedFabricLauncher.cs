using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace RustedFabricLauncher
{
    internal static class Program
    {
        [STAThread]
        private static int Main(string[] args)
        {
            try
            {
                string gameDirectory = AppDomain.CurrentDomain.BaseDirectory.TrimEnd(
                    Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
                string java = FindJava(gameDirectory);
                string provider = FindProvider(gameDirectory);
                string fabricLibraries = Path.Combine(gameDirectory, "fabric-libs", "*");

                ProcessStartInfo start = new ProcessStartInfo();
                start.FileName = java;
                start.WorkingDirectory = gameDirectory;
                start.UseShellExecute = false;
                start.CreateNoWindow = true;
                start.Arguments = BuildArguments(fabricLibraries, provider, args);
                Process.Start(start);
                return 0;
            }
            catch (Exception failure)
            {
                MessageBox.Show(
                    "无法启动 Rusted Fabric Loader。\r\n\r\n" + failure.Message
                    + "\r\n\r\nFailed to launch Rusted Fabric Loader.",
                    "Rusted Fabric Loader", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return 1;
            }
        }

        private static string FindJava(string gameDirectory)
        {
            string javaw = Path.Combine(gameDirectory, "jvm64", "bin", "javaw.exe");
            if (File.Exists(javaw)) return javaw;
            string java = Path.Combine(gameDirectory, "jvm64", "bin", "java.exe");
            if (File.Exists(java)) return java;
            throw new FileNotFoundException("找不到游戏自带的 jvm64\\bin\\java.exe");
        }

        private static string FindProvider(string gameDirectory)
        {
            string directory = Path.Combine(gameDirectory, "rusted-fabric-loader");
            if (!Directory.Exists(directory))
                throw new DirectoryNotFoundException("找不到 rusted-fabric-loader 目录");
            string[] candidates = Directory.GetFiles(directory, "rusted-fabric-loader-*.jar");
            if (candidates.Length == 0)
                throw new FileNotFoundException("找不到 Rusted Fabric Loader GameProvider");
            return candidates.OrderByDescending(File.GetLastWriteTimeUtc).First();
        }

        private static string BuildArguments(string fabricLibraries, string provider, string[] args)
        {
            List<string> values = new List<string>();
            values.Add("-Xmx4096M");
            values.Add("-Dfile.encoding=UTF-8");
            values.Add("-cp");
            values.Add(fabricLibraries + ";" + provider);
            values.Add("net.fabricmc.loader.impl.launch.knot.KnotClient");
            values.AddRange(args);
            return string.Join(" ", values.Select(QuoteArgument).ToArray());
        }

        private static string QuoteArgument(string value)
        {
            if (value.Length > 0 && value.IndexOfAny(new[] { ' ', '\t', '\n', '\v', '"' }) < 0)
                return value;
            StringBuilder result = new StringBuilder();
            result.Append('"');
            int slashes = 0;
            foreach (char character in value)
            {
                if (character == '\\')
                {
                    slashes++;
                }
                else if (character == '"')
                {
                    result.Append('\\', slashes * 2 + 1);
                    result.Append('"');
                    slashes = 0;
                }
                else
                {
                    result.Append('\\', slashes);
                    slashes = 0;
                    result.Append(character);
                }
            }
            result.Append('\\', slashes * 2);
            result.Append('"');
            return result.ToString();
        }
    }
}
