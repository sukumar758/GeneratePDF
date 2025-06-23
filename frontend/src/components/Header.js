
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Header.css';

function Header() {
  const { currentUser, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  return (
    <header className="header">
      <div className="header-container">
        <div className="logo">
          <Link to="/">
            <img src="/img1.png" alt="Acentrik Technology Solutions LLC" />
          </Link>
        </div>
        <nav>
          <ul className="nav-list">
            <li><Link to="/">Home</Link></li>
            {currentUser ? (
              <>
                {currentUser.role === 'ROLE_ADMIN' && (
                  <li><Link to="/dashboard">Dashboard</Link></li>
                )}
                <li><Link to="/profile">My Profile</Link></li>
                <li>
                  <button onClick={handleLogout} className="logout-btn">
                    Logout
                  </button>
                </li>
                <li className="user-info">
                  Welcome, {currentUser.username}!
                </li>
              </>
            ) : (
              <>
                <li><Link to="/login">Login</Link></li>
                <li><Link to="/register">Register</Link></li>
              </>
            )}
          </ul>
        </nav>
      </div>
    </header>
  );
}

export default Header;
