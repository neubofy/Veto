'use client';

export interface StoredAccount {
  uid: string;
  email: string;
  displayName: string;
  photoURL: string;
  isDefault: boolean;
  encryptedCredential?: string; // Optional, maybe we don't need it if we rely on browser session/cookies or we can just prompt sign in.
  // Actually, OAuthCredentials can't be easily stored and re-used for silent sign in without a refresh token.
  // We'll store basic info. The user might need to click "Switch" which might trigger Google popup if the session expired,
  // but usually Firebase Auth persists one session. For multi-account, we might need `signInWithPopup(auth, provider)` with `prompt: 'select_account'`.
}

const ACCOUNTS_KEY = 'veto_accounts';

export const accountManager = {
  getStoredAccounts(): StoredAccount[] {
    if (typeof window === 'undefined') return [];
    const data = localStorage.getItem(ACCOUNTS_KEY);
    if (!data) return [];
    try {
      return JSON.parse(data);
    } catch (e) {
      return [];
    }
  },

  addAccount(account: Omit<StoredAccount, 'isDefault'>): void {
    const accounts = this.getStoredAccounts();
    const existing = accounts.findIndex(a => a.uid === account.uid);
    
    // If it's the first account, make it default
    const isDefault = accounts.length === 0;
    
    if (existing >= 0) {
      accounts[existing] = { ...accounts[existing], ...account, isDefault: accounts[existing].isDefault };
    } else {
      accounts.push({ ...account, isDefault });
    }
    
    localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts));
  },

  removeAccount(uid: string): void {
    let accounts = this.getStoredAccounts();
    const wasDefault = accounts.find(a => a.uid === uid)?.isDefault;
    accounts = accounts.filter(a => a.uid !== uid);
    
    if (wasDefault && accounts.length > 0) {
      accounts[0].isDefault = true;
    }
    
    localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts));
  },

  getDefaultAccountUid(): string | null {
    const accounts = this.getStoredAccounts();
    const def = accounts.find(a => a.isDefault);
    return def ? def.uid : (accounts.length > 0 ? accounts[0].uid : null);
  },

  setDefaultAccount(uid: string): void {
    const accounts = this.getStoredAccounts();
    accounts.forEach(a => {
      a.isDefault = a.uid === uid;
    });
    localStorage.setItem(ACCOUNTS_KEY, JSON.stringify(accounts));
  },

  getActiveAccountUid(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem('veto_active_account_uid') || this.getDefaultAccountUid();
  },

  setActiveAccountUid(uid: string): void {
    if (typeof window === 'undefined') return;
    localStorage.setItem('veto_active_account_uid', uid);
  }
};
