import { NextResponse } from 'next/server';
import { encryptVeto } from '@/lib/vetoCrypto';
import { adminAuth } from '@/lib/firebaseAdmin';

export async function POST(req: Request) {
    try {
        const authHeader = req.headers.get('Authorization');
        if (!authHeader?.startsWith('Bearer ')) {
            return NextResponse.json({ error: 'Missing or invalid token' }, { status: 401 });
        }

        const idToken = authHeader.split('Bearer ')[1];
        const decodedToken = await adminAuth!.verifyIdToken(idToken);
        const uid = decodedToken.uid;

        const body = await req.json();
        const { data, pin } = body;

        if (!data || !pin) {
            return NextResponse.json({ error: 'Missing data or pin' }, { status: 400 });
        }

        const encrypted = encryptVeto(data, pin, uid);
        
        return NextResponse.json({ result: encrypted }, { status: 200 });
    } catch (error: any) {
        console.error('Encryption error:', error);
        return NextResponse.json({ error: 'Encryption failed' }, { status: 500 });
    }
}
