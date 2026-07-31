import { NextResponse } from 'next/server';
import { adminAuth } from '@/lib/firebaseAdmin';

export async function GET(req: Request) {
  try {
    const { searchParams } = new URL(req.url);
    const url = searchParams.get('url');
    const token = searchParams.get('token');
    
    if (!url || !token) {
      return new Response('Missing URL or token', { status: 400 });
    }

    try {
      const parsedUrl = new URL(url);
      if (!parsedUrl.hostname.endsWith('.vercel-storage.com')) {
        return new Response('Invalid image domain', { status: 400 });
      }
    } catch (e) {
      return new Response('Invalid URL format', { status: 400 });
    }

    // Verify Firebase Auth Token
    let decodedToken;
    try {
      decodedToken = await adminAuth.verifyIdToken(token);
    } catch (e) {
      return new Response('Invalid authentication token', { status: 401 });
    }

    // Fetch the private blob using the Vercel Blob Read/Write Token
    const blobToken = process.env.BLOB_READ_WRITE_TOKEN;
    if (!blobToken) {
      return new Response('Missing blob token configuration', { status: 500 });
    }

    const response = await fetch(url, {
      headers: {
        Authorization: `Bearer ${blobToken}`,
      },
    });

    if (!response.ok) {
      return new Response('Failed to fetch image', { status: response.status });
    }

    // Proxy the image back to the client
    const contentType = response.headers.get('content-type') || 'image/jpeg';
    const buffer = await response.arrayBuffer();

    return new Response(buffer, {
      headers: {
        'Content-Type': contentType,
        'Cache-Control': 'public, max-age=86400', // Cache for 1 day
      },
    });

  } catch (error: any) {
    console.error('Image proxy error:', error);
    return new Response('Internal Server Error', { status: 500 });
  }
}
