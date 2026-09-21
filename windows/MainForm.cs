using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.IO;
using System.Windows.Forms;

namespace CinejoyTV;

public sealed class MainForm : Form
{
    private const string HomeUrl = "https://cinejoy.pk/";
    private const string AllowedHost = "cinejoy.pk";

    private readonly WebView2 webView = new();
    private readonly HashSet<string> blockedHosts = new(StringComparer.OrdinalIgnoreCase)
    {
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "adservice.google.com",
        "googletagmanager.com",
        "googletagservices.com",
        "amazon-adsystem.com",
        "adsrvr.org",
        "adnxs.com",
        "taboola.com",
        "outbrain.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "exoclick.com",
        "onclickperformance.com",
        "trafficjunky.com",
        "pypo.com",
        "pyppo.com",
        "adf.ly",
        "adfly",
        "ouo.io",
        "ouo.press",
        "shrinkme.io",
        "shrinkearn.com"
    };

    private string lastSafeUrl = HomeUrl;
    private bool initialized;

    public MainForm()
    {
        Text = "Cinejoy";
        StartPosition = FormStartPosition.CenterScreen;
        WindowState = FormWindowState.Maximized;
        FormBorderStyle = FormBorderStyle.None;
        BackColor = Color.Black;
        KeyPreview = true;

        webView.Dock = DockStyle.Fill;
        Controls.Add(webView);

        KeyDown += MainForm_KeyDown;
        Load += async (_, _) => await InitializeBrowserAsync();
    }

    private static bool IsCinejoyHost(string? url)
    {
        if (string.IsNullOrWhiteSpace(url) ||
            !Uri.TryCreate(url, UriKind.Absolute, out var uri))
            return false;

        return uri.Scheme.Equals(Uri.UriSchemeHttps, StringComparison.OrdinalIgnoreCase)
            && (uri.Host.Equals(AllowedHost, StringComparison.OrdinalIgnoreCase)
                || uri.Host.Equals("www." + AllowedHost, StringComparison.OrdinalIgnoreCase));
    }

    private bool IsBlockedHost(string? url)
    {
        if (string.IsNullOrWhiteSpace(url) ||
            !Uri.TryCreate(url, UriKind.Absolute, out var uri))
            return false;

        foreach (var blocked in blockedHosts)
        {
            if (uri.Host.Equals(blocked, StringComparison.OrdinalIgnoreCase) ||
                uri.Host.EndsWith("." + blocked, StringComparison.OrdinalIgnoreCase))
                return true;
        }

        return false;
    }

    private static bool IsBadScheme(string? url)
    {
        if (string.IsNullOrWhiteSpace(url))
            return true;

        var value = url.Trim();

        return value.StartsWith("intent:", StringComparison.OrdinalIgnoreCase)
            || value.StartsWith("market:", StringComparison.OrdinalIgnoreCase)
            || value.StartsWith("javascript:", StringComparison.OrdinalIgnoreCase)
            || value.StartsWith("tel:", StringComparison.OrdinalIgnoreCase)
            || value.StartsWith("mailto:", StringComparison.OrdinalIgnoreCase)
            || value.StartsWith("whatsapp:", StringComparison.OrdinalIgnoreCase)
            || value.StartsWith("tg:", StringComparison.OrdinalIgnoreCase)
            || value.StartsWith("viber:", StringComparison.OrdinalIgnoreCase);
    }

    private bool ShouldBlockNavigation(string? url)
    {
        if (IsBadScheme(url) || IsBlockedHost(url))
            return true;

        // Keep the top-level WebView locked to Cinejoy.
        return !IsCinejoyHost(url);
    }

    private async System.Threading.Tasks.Task InitializeBrowserAsync()
    {
        if (initialized)
            return;

        initialized = true;

        try
        {
            var userDataFolder = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "CinejoyTV",
                "WebView2");

            var environment = await CoreWebView2Environment.CreateAsync(
                browserExecutableFolder: null,
                userDataFolder: userDataFolder);

            await webView.EnsureCoreWebView2Async(environment);

            ConfigureWebView();

            webView.CoreWebView2.Navigate(HomeUrl);
        }
        catch (Exception ex)
        {
            MessageBox.Show(
                "Cinejoy could not start. Make sure Microsoft Edge WebView2 Runtime is installed.\n\n" + ex.Message,
                "Cinejoy",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error);
        }
    }

    private void ConfigureWebView()
    {
        var core = webView.CoreWebView2;

        core.Settings.AreDevToolsEnabled = false;
        core.Settings.AreDefaultContextMenusEnabled = false;
        core.Settings.IsStatusBarEnabled = false;
        core.Settings.IsZoomControlEnabled = false;

        // Block unwanted top-level redirects.
        core.NavigationStarting += (_, args) =>
        {
            var url = args.Uri;

            if (ShouldBlockNavigation(url))
            {
                args.Cancel = true;

                if (!string.IsNullOrWhiteSpace(lastSafeUrl) &&
                    IsCinejoyHost(lastSafeUrl) &&
                    !string.Equals(url, lastSafeUrl, StringComparison.OrdinalIgnoreCase))
                {
                    BeginInvoke(new Action(() =>
                    {
                        if (!string.Equals(core.Source, lastSafeUrl, StringComparison.OrdinalIgnoreCase))
                            core.Navigate(lastSafeUrl);
                    }));
                }

                return;
            }

            lastSafeUrl = url;
        };

        // Do not allow window.open(), target=_blank, or popup windows.
        core.NewWindowRequested += (_, args) =>
        {
            args.Handled = true;
        };

        // Block known ad/tracker resource hosts while allowing normal external
        // media/resources to load.
        core.AddWebResourceRequestedFilter(
            "*",
            CoreWebView2WebResourceContext.All);

        core.WebResourceRequested += (_, args) =>
        {
            if (IsBlockedHost(args.Request.Uri))
            {
                args.Response = core.Environment.CreateWebResourceResponse(
                    new MemoryStream(Array.Empty<byte>()),
                    204,
                    "Blocked",
                    "Content-Type: text/plain");
            }
        };

        // Inject a small client-side guard against common popup patterns.
        core.AddScriptToExecuteOnDocumentCreatedAsync("""
            (() => {
                const originalOpen = window.open;
                window.open = function() { return null; };

                document.addEventListener('click', function (e) {
                    const a = e.target && e.target.closest ? e.target.closest('a') : null;
                    if (!a) return;

                    const href = a.href || '';
                    if (/^(intent:|market:|javascript:|tel:|mailto:|whatsapp:|tg:|viber:)/i.test(href)) {
                        e.preventDefault();
                        e.stopImmediatePropagation();
                        return false;
                    }

                    try {
                        const u = new URL(href, location.href);
                        if (u.hostname !== 'cinejoy.pk' &&
                            u.hostname !== 'www.cinejoy.pk') {
                            e.preventDefault();
                            e.stopImmediatePropagation();
                            return false;
                        }
                    } catch (_) {}
                }, true);
            })();
        """);
    }

    private void MainForm_KeyDown(object? sender, KeyEventArgs e)
    {
        if (e.KeyCode == Keys.F5)
        {
            webView.CoreWebView2?.Reload();
            e.Handled = true;
            return;
        }

        if (e.Alt && e.KeyCode == Keys.Left &&
            webView.CoreWebView2?.CanGoBack == true)
        {
            webView.CoreWebView2.GoBack();
            e.Handled = true;
            return;
        }

        if (e.KeyCode == Keys.F11)
        {
            WindowState = WindowState == FormWindowState.Maximized
                ? FormWindowState.Normal
                : FormWindowState.Maximized;
            FormBorderStyle = FormBorderStyle.None;
            e.Handled = true;
        }
    }
}
