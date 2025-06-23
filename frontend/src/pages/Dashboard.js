
import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './Dashboard.css';

function Dashboard() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [newUser, setNewUser] = useState({ username: '', password: '' });

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    try {
      const response = await axios.get('/api/users');
      setUsers(response.data);
    } catch (error) {
      setError('Failed to fetch users');
      console.error('Error fetching users:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddUser = async (e) => {
    e.preventDefault();
    try {
      const response = await axios.post('/api/users', newUser);
      setSuccess(response.data.message);
      setNewUser({ username: '', password: '' });
      fetchUsers();
      setError('');
    } catch (error) {
      setError(error.response?.data?.error || 'Failed to add user');
      setSuccess('');
    }
  };

  const handleDeleteUser = async (userId) => {
    if (window.confirm('Are you sure you want to delete this user?')) {
      try {
        await axios.delete(`/api/users/${userId}`);
        setSuccess('User deleted successfully');
        fetchUsers();
        setError('');
      } catch (error) {
        setError(error.response?.data?.error || 'Failed to delete user');
        setSuccess('');
      }
    }
  };

  if (loading) return <div className="loading">Loading...</div>;

  return (
    <div className="dashboard">
      <h1>Admin Dashboard</h1>
      
      {error && <div className="error-message">{error}</div>}
      {success && <div className="success-message">{success}</div>}
      
      <div className="dashboard-section">
        <h2>Add New Employee</h2>
        <form onSubmit={handleAddUser} className="add-user-form">
          <div className="form-group">
            <label htmlFor="newUsername">Username:</label>
            <input
              type="text"
              id="newUsername"
              value={newUser.username}
              onChange={(e) => setNewUser({...newUser, username: e.target.value})}
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="newPassword">Password:</label>
            <input
              type="password"
              id="newPassword"
              value={newUser.password}
              onChange={(e) => setNewUser({...newUser, password: e.target.value})}
              required
            />
          </div>
          <button type="submit" className="add-button">Add Employee</button>
        </form>
      </div>

      <div className="dashboard-section">
        <h2>Employee List</h2>
        <div className="table-container">
          <table className="user-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Username</th>
                <th>Role</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {users.map(user => (
                <tr key={user.id}>
                  <td>{user.id}</td>
                  <td>{user.username}</td>
                  <td>{user.role}</td>
                  <td>
                    <button
                      onClick={() => handleDeleteUser(user.id)}
                      className="delete-button"
                      disabled={user.role === 'ROLE_ADMIN'}
                    >
                      {user.role === 'ROLE_ADMIN' ? 'Protected' : 'Delete'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
