
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './Profile.css';

function Profile() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [passwordData, setPasswordData] = useState({
    newPassword: '',
    confirmPassword: ''
  });

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const response = await axios.get('/api/profile');
      setProfile(response.data);
    } catch (error) {
      setError('Failed to fetch profile');
      console.error('Error fetching profile:', error);
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordUpdate = async (e) => {
    e.preventDefault();
    
    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    try {
      const response = await axios.put('/api/profile/password', passwordData);
      setSuccess(response.data.message);
      setPasswordData({ newPassword: '', confirmPassword: '' });
      setError('');
    } catch (error) {
      setError(error.response?.data?.error || 'Failed to update password');
      setSuccess('');
    }
  };

  if (loading) return <div className="loading">Loading...</div>;

  return (
    <div className="profile">
      <h1>My Profile</h1>
      
      {error && <div className="error-message">{error}</div>}
      {success && <div className="success-message">{success}</div>}
      
      {profile && (
        <>
          <div className="profile-section">
            <h2>Profile Information</h2>
            <div className="profile-info">
              <p><strong>Username:</strong> {profile.username}</p>
              <p><strong>Role:</strong> {profile.role}</p>
              <p><strong>Offer Letter:</strong> {profile.hasOfferLetter ? 'Available' : 'Not Available'}</p>
            </div>
          </div>

          <div className="profile-section">
            <h2>Update Password</h2>
            <form onSubmit={handlePasswordUpdate} className="password-form">
              <div className="form-group">
                <label htmlFor="newPassword">New Password:</label>
                <input
                  type="password"
                  id="newPassword"
                  value={passwordData.newPassword}
                  onChange={(e) => setPasswordData({
                    ...passwordData, 
                    newPassword: e.target.value
                  })}
                  required
                />
              </div>
              
              <div className="form-group">
                <label htmlFor="confirmPassword">Confirm Password:</label>
                <input
                  type="password"
                  id="confirmPassword"
                  value={passwordData.confirmPassword}
                  onChange={(e) => setPasswordData({
                    ...passwordData, 
                    confirmPassword: e.target.value
                  })}
                  required
                />
              </div>
              
              <button type="submit" className="update-button">
                Update Password
              </button>
            </form>
          </div>
        </>
      )}
    </div>
  );
}

export default Profile;
