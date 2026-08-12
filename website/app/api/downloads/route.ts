import { NextResponse } from 'next/server';

export const revalidate = 3600; // Cache for 1 hour

export async function GET() {
  try {
    let page = 1;
    let totalDownloads = 0;
    let hasMore = true;

    while (hasMore) {
      const res = await fetch(`https://api.github.com/repos/neubofy/Veto/releases?per_page=100&page=${page}`, {
        headers: {
          'User-Agent': 'Veto-Website',
          'Accept': 'application/vnd.github.v3+json'
        },
        next: { revalidate: 3600 }
      });

      if (!res.ok) {
        throw new Error(`GitHub API responded with ${res.status}`);
      }

      const releases = await res.json();
      
      if (releases.length === 0) {
        hasMore = false;
        break;
      }

      for (const release of releases) {
        if (release.assets && release.assets.length > 0) {
          for (const asset of release.assets) {
            totalDownloads += asset.download_count || 0;
          }
        }
      }
      
      if (releases.length < 100) {
        hasMore = false;
      } else {
        page++;
      }
    }

    return NextResponse.json({ downloads: totalDownloads });

  } catch (error) {
    console.error('Error fetching download counts:', error);
    return NextResponse.json({ downloads: null }, { status: 500 });
  }
}
