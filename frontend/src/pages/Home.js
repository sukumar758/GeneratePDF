
import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import './Home.css';

function Home() {
  const { currentUser } = useAuth();

  return (
    <div className="home">
      <section className="hero">
        <div className="tech-pattern-bg"></div>
        <h1 className="animate-slide-up">We serve you the software solutions</h1>
        <p className="animate-slide-up">
          Explore our services and products tailored for innovation and success.
        </p>
        
        {currentUser && (
          <div className="user-info animate-fade-in">
            <h3>Welcome, {currentUser.username}!</h3>
            <p>You are logged in as: {currentUser.role}</p>
            {currentUser.role === 'ROLE_ADMIN' && (
              <p>You have admin privileges.</p>
            )}
            {currentUser.role === 'ROLE_USER' && (
              <p>You have user privileges.</p>
            )}
          </div>
        )}
      </section>

      <section className="testimonials">
        <h2 className="section-title">What Our Clients Say</h2>
        <div className="testimonial-container">
          <div className="testimonial-card">
            <div className="testimonial-content">
              <i className="fas fa-quote-left quote-icon"></i>
              <p>
                "Acentrik Technology Solutions transformed our business with their 
                innovative software. Their team's expertise and dedication exceeded 
                our expectations."
              </p>
              <i className="fas fa-quote-right quote-icon right"></i>
            </div>
            <div className="testimonial-author">
              <img 
                src="https://randomuser.me/api/portraits/men/32.jpg" 
                alt="John Smith" 
                className="testimonial-avatar"
              />
              <div className="author-info">
                <h4>John Smith</h4>
                <p>CEO, TechInnovate Inc.</p>
              </div>
            </div>
          </div>

          <div className="testimonial-card">
            <div className="testimonial-content">
              <i className="fas fa-quote-left quote-icon"></i>
              <p>
                "Working with Acentrik has been a game-changer for our company. 
                Their solutions are not only cutting-edge but also user-friendly 
                and reliable."
              </p>
              <i className="fas fa-quote-right quote-icon right"></i>
            </div>
            <div className="testimonial-author">
              <img 
                src="https://randomuser.me/api/portraits/women/44.jpg" 
                alt="Sarah Johnson" 
                className="testimonial-avatar"
              />
              <div class="author-info">
                <h4>Sarah Johnson</h4>
                <p>CTO, Digital Dynamics</p>
              </div>
            </div>
          </div>

          <div className="testimonial-card">
            <div className="testimonial-content">
              <i className="fas fa-quote-left quote-icon"></i>
              <p>
                "The team at Acentrik delivered our project on time and within budget. 
                Their attention to detail and customer service is unmatched in the industry."
              </p>
              <i className="fas fa-quote-right quote-icon right"></i>
            </div>
            <div className="testimonial-author">
              <img 
                src="https://randomuser.me/api/portraits/men/67.jpg" 
                alt="Michael Chen" 
                className="testimonial-avatar"
              />
              <div className="author-info">
                <h4>Michael Chen</h4>
                <p>Director, Global Solutions</p>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
}

export default Home;
