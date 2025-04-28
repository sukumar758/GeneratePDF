// Animation script for Acentrik website
document.addEventListener('DOMContentLoaded', function() {
    // Check for reduced motion preference
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Function to check if an element is in viewport with a margin
    function isInViewport(element, margin = 0) {
        const rect = element.getBoundingClientRect();
        return (
            rect.top <= (window.innerHeight || document.documentElement.clientHeight) + margin &&
            rect.bottom >= 0 - margin &&
            rect.left <= (window.innerWidth || document.documentElement.clientWidth) + margin &&
            rect.right >= 0 - margin
        );
    }

    // Function to handle scroll animations with improved performance
    function handleScrollAnimations() {
        // Skip animations if user prefers reduced motion
        if (prefersReducedMotion) return;

        // Get all elements with animation classes that should be triggered on scroll
        const animatedElements = document.querySelectorAll('.testimonial-card, .section-title, .hero h1, .hero p, .animate-on-scroll, .tech-image');

        animatedElements.forEach((element, index) => {
            // Only animate if element is in viewport and not already animated
            if (isInViewport(element, 100) && !element.classList.contains('animated')) {
                // Add 'animated' class to prevent re-animation
                element.classList.add('animated');

                // Determine the animation class based on element type
                let animationClass = 'animate-fade-in';

                if (element.classList.contains('testimonial-card')) {
                    animationClass = 'animate-slide-in';
                    // Add staggered delay for testimonial cards
                    const delay = 0.1 * (index % 3); // Stagger by index within its type
                    element.style.animationDelay = `${delay}s`;
                } else if (element.classList.contains('hero-image')) {
                    animationClass = 'animate-zoom-in';
                } else if (element.classList.contains('tech-image')) {
                    animationClass = 'animate-float';
                } else if (element.tagName === 'H1' && element.closest('.hero')) {
                    animationClass = 'animate-slide-up';
                } else if (element.tagName === 'P' && element.closest('.hero')) {
                    animationClass = 'animate-slide-up';
                    // Add a slight delay for the paragraph
                    element.style.animationDelay = '0.3s';
                } else if (element.classList.contains('animate-on-scroll')) {
                    // Add staggered delay for table rows
                    const delay = 0.05 * index;
                    element.style.animationDelay = `${delay}s`;
                }

                // Apply the animation class (no need to remove and re-add)
                if (!element.classList.contains(animationClass)) {
                    element.classList.add(animationClass);
                }
            }
        });
    }

    // Add hover effect for testimonial cards with more subtle animation
    const testimonialCards = document.querySelectorAll('.testimonial-card');
    testimonialCards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            if (!prefersReducedMotion) {
                this.style.transform = 'translateY(-5px)'; // Reduced from -10px
                this.style.boxShadow = 'var(--shadow-lg)';
                this.style.transition = 'transform 0.3s ease-out, box-shadow 0.3s ease';
            }
        });

        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0)';
            this.style.boxShadow = 'var(--shadow-md)';
            this.style.transition = 'transform 0.3s ease, box-shadow 0.3s ease';
        });
    });

    // Add hover effect for buttons with more subtle animation
    const buttons = document.querySelectorAll('.btn');
    buttons.forEach(button => {
        button.addEventListener('mouseenter', function() {
            if (!prefersReducedMotion) {
                this.style.transform = 'translateY(-2px) scale(1.02)'; // Reduced from -3px and 1.05
                this.style.boxShadow = 'var(--shadow-lg)';
                this.style.transition = 'all 0.3s ease-out';
            }
        });

        button.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0) scale(1)';
            this.style.boxShadow = 'var(--shadow-md)';
            this.style.transition = 'all 0.3s ease';
        });
    });

    // Add parallax effect for tech pattern background with reduced motion consideration
    const techPattern = document.querySelector('.tech-pattern-bg');
    if (techPattern && !prefersReducedMotion) {
        window.addEventListener('scroll', function() {
            // Use requestAnimationFrame for better performance
            requestAnimationFrame(() => {
                const scrollPosition = window.pageYOffset;
                techPattern.style.transform = `translateY(${scrollPosition * 0.1}px)`; // Reduced from 0.2
            });
        });
    }

    // Add floating animation for tech images with improved performance
    const techImages = document.querySelectorAll('.tech-image');
    let floatAnimationId;

    function animateFloat(timestamp) {
        if (!lastTimestamp) lastTimestamp = timestamp;
        const elapsed = timestamp - lastTimestamp;

        // Only update every ~16ms (60fps) or more
        if (elapsed > 16) {
            lastTimestamp = timestamp;
            counter += 0.01; // Reduced from 0.05 for smoother animation

            techImages.forEach((img, index) => {
                // Add a phase shift for each image to create a wave effect
                const phaseShift = index * (Math.PI / 4);
                const yPos = Math.sin(counter + phaseShift) * 6; // Reduced from 10px
                img.style.transform = `translateY(${yPos}px)`;
            });
        }

        floatAnimationId = requestAnimationFrame(animateFloat);
    }

    let counter = 0;
    let lastTimestamp = 0;

    // Start floating animation if there are tech images and user doesn't prefer reduced motion
    if (techImages.length > 0 && !prefersReducedMotion) {
        floatAnimationId = requestAnimationFrame(animateFloat);
    }

    // Clean up animation on page unload
    window.addEventListener('beforeunload', () => {
        if (floatAnimationId) {
            cancelAnimationFrame(floatAnimationId);
        }
    });

    // Initial check for elements in viewport
    handleScrollAnimations();

    // Listen for scroll events with throttling for better performance
    let scrollTimeout;
    window.addEventListener('scroll', function() {
        if (scrollTimeout) return;

        scrollTimeout = setTimeout(() => {
            handleScrollAnimations();
            scrollTimeout = null;
        }, 100); // Throttle to once every 100ms
    });

    // Also check on resize
    window.addEventListener('resize', handleScrollAnimations);
});
