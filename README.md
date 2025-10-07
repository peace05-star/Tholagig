# TholaGig - Find Gigs. Earn Money. Build Your Future.

*Connecting Talent with Opportunity in South Africa*

**Github Link**: https://github.com/peace05-star/Tholagig.git 

**School Github Link**: https://github.com/IIEMSA/opsc6312-poe-part-2-Peacesalomyphiri.git

**Vimeo Demo Video Link**: [https://youtu.be/6-ASEepyqAQ?feature=shared](https://vimeo.com/1125288731?share=copy)

## Introduction
South Africa has one of the largest informal labour markets in the world. Millions of people rely on "piece jobs" or as locals call them, skropes. These are temporary or once-off jobs that provide short-term income but often lack structure, visibility, and dignity.
While global freelancing platforms like Fiverr and Freelancer exist, they are not widely known or relevant to the South African majority. They do not speak the language of the people or reflect the realities of working in townships, rural communities, or low-data environments.
TholaGig fills this gap.
"Thola" means find in isiZulu. TholaGig is a comprehensive freelance marketplace mobile application built for Android that connects businesses and individuals (clients) with skilled professionals (freelancers). Designed specifically for the South African market, the app facilitates project posting, talent discovery, application management, and seamless communication between parties.
## Purpose of the App
TholaGig is about formalizing the informal.
It brings structure and dignity to the skrope economy. It helps workers track gigs, payments, and professional growth. It introduces freelancing as a career path, not just a survival strategy.
## Vision for Growth:
-	**Now**: Empowering disadvantaged and low-skilled workers with tools to track and grow their work.
-	**Next**: Expanding to serve higher-skilled professionals such as developers, designers, and lawyers.
-	**Future**: Becoming a trusted South African freelancing ecosystem that levels the global playing field.

## The Problem
-	A large informal labour sector with little organization.
-	Lack of local, relatable platforms for gig workers.
-	High youth unemployment and need for flexible income opportunities.
-	Limited awareness of global freelancing platforms in South Africa.

## The Solution
TholaGig provides:
-	Relatable branding (local language, culture, and context).
-	Accessibility with offline-first features and low-data optimization.
-	Empowerment by enabling users to showcase skills, track earnings, and build credibility.
-	Inclusivity through multi-language support (English + isiZulu) which will be done in the final part

## Project Structure
 
## MVVM Pattern
View (Activity/Fragment) → ViewModel → Repository → Data Sources
                                      ↓
                              REST API + Firebase Services
## Architecture Overview
The TholaGig app follows modular architecture with a clean package organization that separates responsibilities by features such as authentication, dashboards, jobs, messaging, profiles, and networking.
## Technical Implementation
Authentication and user management are handled via Firebase Authentication, providing secure login and registration flows with role-based access (Client/Freelancer). Job management, messaging, and profile handling are built with a combination of Firebase and REST APIs using Retrofit. MVVM architecture ensures a clear separation of concerns between the UI, business logic, and data sources.
## Technology Stack
-	Firebase Authentication
-	Firebase Firestore
-	Retrofit 2 + Gson Converter
-	Android Jetpack Components (ViewModel, LiveData)
-	RecyclerView + Material Components

## Data Flow Architecture
-	MVVM Pattern Implementation:
-	View (Activity/Fragment) → ViewModel → Repository → Data Sources
-	Data Sources include both local (Firebase/Room) and remote (REST API) components.

## Key Features
-	Authentication and Role Management
-	Job Creation and Browsing
-	 Application System for Freelancers
-	 Messaging System
-	 Profile Management
-	 REST API and Firebase Integration

## Security & Session Management
SessionManager.kt manages authentication tokens, login persistence, and session clearing. All network communication is secured using HTTPS, and Firebase handles authentication with encrypted credentials.
## Getting Started
1.	Clone the repository
2.	 Add google-services.json to the app directory
3.	Configure API base URL in ApiClient.kt
4.	Build and run the application in Android Studio
## GitHub Repository
Current Status
-	Branches: 3 active branches
-	Commits: 33 commits
-	Latest Update: REST API integration for ClientProfileActivity
  
  Recent Development
-	Implemented: REST API integration using Retrofit for client profiles
-	Enhanced: ClientProfileActivity with API data binding
  
  In Progress
- Expanded API integration for other features
## Contributing Guidelines
We welcome contributions from the developer community!
## Development Process
1.	Fork the repository
2.	Create a feature branch: git checkout -b feature/your-feature
3.	Commit changes: Use conventional commit messages
4.	Push and open Pull Request
## Branch Naming
- feature/ - New features
## Project Status
Current Version
## Phase 2 - Core Implementation
## Completed Features
-	Firebase Authentication system
-	REST API integration with Retrofit
-	Client and freelancer profiles
-	Job posting and browsing
-	Basic messaging system
## In Development
-	Enhanced job application flow
-	Real-time notifications
-	Payment integration setup
-	Advanced search and filtering
## Phase 3 Roadmap
-	Offline functionality with Room DB
-	QR code networking
-	Gamification system
-	Advanced analytics
## License
This project is licensed under Tholagig’s Developers.
## Development Team
-	Ndumiso Magwanya - Project Lead & Backend Development
-	Peace Salomy Phiri - UI/UX Design & Frontend Development
-	Sinazo Happy Mgidi - API Development & Integration
-	Sindiswa Nomakholwa Madliwa - Mobile Development & Architecture


