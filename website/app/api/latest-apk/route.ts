import { NextResponse } from 'next/server';

export const dynamic = 'force-dynamic'; // Prevent static caching to ensure latest release is fetched

export async function GET() {
  try {
    const res = await fetch('https://api.github.com/repos/neubofy/Veto/releases/latest', {
      headers: {
        'User-Agent': 'Veto-Website',
        'Accept': 'application/vnd.github.v3+json'
      }
    });

    if (!res.ok) {
      throw new Error(`GitHub API responded with ${res.status}`);
    }

    const data = await res.json();
    const apkAssets = data.assets.filter((a: { name: string, browser_download_url: string }) => a.name.endsWith('.apk'));

    if (apkAssets.length === 0) {
      // Fallback if no APK is found in the latest release
      return NextResponse.redirect('https://github.com/neubofy/Veto/releases/latest');
    }

    // Sort by timestamp if there are multiple APKs
    apkAssets.sort((a: { name: string, browser_download_url: string }, b: { name: string, browser_download_url: string }) => {
      const tsA = parseInt(a.name.match(/-(\d+)-/)?.[1] || '0');
      const tsB = parseInt(b.name.match(/-(\d+)-/)?.[1] || '0');
      return tsB - tsA; // Descending
    });

    const latestApkUrl = apkAssets[0].browser_download_url;
    return NextResponse.redirect(latestApkUrl);

  } catch (error) {
    console.error('Error fetching latest APK:', error);
    // Fallback on error
    return NextResponse.redirect('https://github.com/neubofy/Veto/releases/latest');
  }
}
