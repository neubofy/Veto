'use client';

import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown, Check, Star, LogOut, Plus, Trash2 } from 'lucide-react';
import { accountManager, StoredAccount } from '@/lib/accountManager';
import { auth } from '@/lib/firebaseClient';
import { GoogleAuthProvider, signInWithPopup } from 'firebase/auth';

interface AccountSwitcherProps {
  currentAccount: StoredAccount | null;
  onAccountSwitch: (account: StoredAccount) => void;
  onLogoutCurrent: () => void;
}

export default function AccountSwitcher({ currentAccount, onAccountSwitch, onLogoutCurrent }: AccountSwitcherProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [accounts, setAccounts] = useState<StoredAccount[]>([]);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setAccounts(accountManager.getStoredAccounts());
    
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };
    
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isOpen]);

  const handleSwitch = async (account: StoredAccount) => {
    if (currentAccount?.uid === account.uid) {
      setIsOpen(false);
      return;
    }
    
    try {
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: 'select_account' });
      await signInWithPopup(auth, provider);
      onAccountSwitch(account);
      setIsOpen(false);
    } catch (e) {
      console.error('Failed to switch account:', e);
    }
  };

  const handleAddAccount = async () => {
    try {
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: 'select_account' });
      const result = await signInWithPopup(auth, provider);
      
      const newAccount: Omit<StoredAccount, 'isDefault'> = {
        uid: result.user.uid,
        email: result.user.email || '',
        displayName: result.user.displayName || 'Unknown User',
        photoURL: result.user.photoURL || '',
      };
      
      accountManager.addAccount(newAccount);
      setAccounts(accountManager.getStoredAccounts());
      
      const stored = accountManager.getStoredAccounts().find(a => a.uid === result.user.uid);
      if (stored) {
        onAccountSwitch(stored);
      }
      setIsOpen(false);
    } catch (e) {
      console.error('Failed to add account:', e);
    }
  };

  const handleRemove = (e: React.MouseEvent, uid: string) => {
    e.stopPropagation();
    if (confirm('Are you sure you want to remove this account?')) {
      accountManager.removeAccount(uid);
      setAccounts(accountManager.getStoredAccounts());
      if (currentAccount?.uid === uid) {
        onLogoutCurrent();
      }
    }
  };

  const handleSetDefault = (e: React.MouseEvent, uid: string) => {
    e.stopPropagation();
    accountManager.setDefaultAccount(uid);
    setAccounts(accountManager.getStoredAccounts());
  };

  if (!currentAccount) return null;

  const otherAccounts = accounts.filter(a => a.uid !== currentAccount.uid);

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="account-switcher-trigger"
      >
        <img
          src={currentAccount.photoURL || `https://ui-avatars.com/api/?name=${encodeURIComponent(currentAccount.displayName)}`}
          alt="Profile"
        />
      </button>

      <AnimatePresence>
        {isOpen && (
          <div className="modal-overlay">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              style={{ position: 'absolute', inset: 0 }}
              onClick={() => setIsOpen(false)}
            />
            
            <motion.div
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              transition={{ type: "spring", duration: 0.5, bounce: 0.3 }}
              className="modal-content"
            >
              {/* Close Button */}
              <button 
                onClick={() => setIsOpen(false)}
                style={{ position: 'absolute', right: '16px', top: '16px', background: 'transparent', border: 'none', color: '#9ca3af', cursor: 'pointer', zIndex: 10 }}
              >
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
              </button>

              {/* Current Account Header */}
              <div className="modal-header">
                <img
                  src={currentAccount.photoURL || `https://ui-avatars.com/api/?name=${encodeURIComponent(currentAccount.displayName)}`}
                  alt=""
                />
                <h3>{currentAccount.displayName}</h3>
                <p>{currentAccount.email}</p>
                
                <button 
                  onClick={() => {}} 
                  className="manage-btn"
                >
                  Manage your Veto Account
                </button>
              </div>

              {/* Other Accounts List */}
              {otherAccounts.length > 0 && (
                <div className="account-list custom-scrollbar">
                  {otherAccounts.map((account) => (
                    <div
                      key={account.uid}
                      onClick={() => handleSwitch(account)}
                      className="account-item"
                    >
                      <div className="account-item-left">
                        <img
                          src={account.photoURL || `https://ui-avatars.com/api/?name=${encodeURIComponent(account.displayName)}`}
                          alt=""
                        />
                        <div className="account-item-details">
                          <span className="account-item-name">{account.displayName}</span>
                          <span className="account-item-email">{account.email}</span>
                        </div>
                      </div>
                      <button
                        onClick={(e) => handleRemove(e, account.uid)}
                        style={{ background: 'transparent', border: 'none', color: '#6b7280', cursor: 'pointer', padding: '8px' }}
                        title="Remove account"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              {/* Actions */}
              <div className="modal-actions">
                <button
                  onClick={handleAddAccount}
                  className="action-btn"
                >
                  <div className="action-icon">
                    <Plus className="h-5 w-5" />
                  </div>
                  Add another account
                </button>
                
                <button
                  onClick={onLogoutCurrent}
                  className="action-btn"
                >
                  <div className="action-icon">
                    <LogOut className="h-5 w-5" />
                  </div>
                  Sign out
                </button>
              </div>
              
              {/* Footer */}
              <div className="modal-footer">
                <a href="/privacy">Privacy Policy</a>
                <span>•</span>
                <a href="/terms">Terms of Service</a>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
