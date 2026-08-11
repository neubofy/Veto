import { initializeApp, getApps, getApp } from 'firebase/app';
import { getAuth, setPersistence, browserLocalPersistence, Auth } from 'firebase/auth';
import { getFirestore, Firestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
};

export interface AccountFirebaseInstance {
  auth: Auth;
  db: Firestore;
}

const instancesMap = new Map<string, AccountFirebaseInstance>();

/**
 * Retrieves or initializes a persistent Firebase App instance for a specific account UID or '[DEFAULT]'.
 */
export function getAccountFirebase(uid?: string | null): AccountFirebaseInstance {
  const appName = uid ? `veto_account_${uid}` : '[DEFAULT]';

  if (instancesMap.has(appName)) {
    return instancesMap.get(appName)!;
  }

  if (!process.env.NEXT_PUBLIC_FIREBASE_API_KEY) {
    return { auth: null as any, db: null as any };
  }

  const existingApp = getApps().find(a => a.name === appName);
  const app = existingApp || initializeApp(firebaseConfig, appName);
  const auth = getAuth(app);

  if (typeof window !== 'undefined') {
    setPersistence(auth, browserLocalPersistence).catch((err) => {
      console.error(`Firebase Auth Persistence Error for ${appName}:`, err);
    });
  }

  const db = getFirestore(app);
  const instance = { auth, db };
  instancesMap.set(appName, instance);

  return instance;
}
