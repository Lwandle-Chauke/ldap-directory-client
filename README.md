<div align="center">

# LDAP Asset Directory Client

### A Java LDAP client that communicates directly with an LDAP server using raw socket programming and the Lightweight Directory Access Protocol (LDAP).

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![LDAP](https://img.shields.io/badge/LDAP-Directory%20Services-blue?style=for-the-badge)
![TCP](https://img.shields.io/badge/TCP-Sockets-success?style=for-the-badge)
![Networking](https://img.shields.io/badge/Computer-Networks-orange?style=for-the-badge)

**Author:** **Lwandle Chauke**

</div>

---

# Overview

The **LDAP Asset Directory Client** is a Java networking application that implements an LDAP client capable of communicating directly with an LDAP server using raw TCP sockets.

Unlike traditional LDAP applications that rely on high-level libraries, this project manually constructs LDAP request packets, transmits them to an LDAP server, and interprets the binary responses according to the LDAP protocol specification.

The application searches an LDAP directory containing organisational assets and retrieves information about a selected asset, including its maximum speed.

This project provided valuable experience with directory services, binary protocol implementation, socket programming, and low-level client-server communication.

---

# Project Highlights

- LDAP client implementation from scratch
- Raw TCP socket communication
- Manual LDAP packet construction
- Binary protocol processing
- Directory Information Tree (DIT) querying
- ASN.1 encoded messages
- BER decoding
- Asset directory lookup
- Java networking
- No LDAP libraries used

---

# Features

## LDAP Client

- Establishes TCP connections to an LDAP server
- Connects using the standard LDAP port
- Sends LDAP search requests
- Receives binary LDAP responses
- Parses server responses

---

## Asset Lookup

- Search for assets by name
- Retrieve maximum speed
- Handle missing entries
- Display formatted search results
- Query custom directory entries

---

## Directory Services

- Communicates with an LDAP server
- Queries Directory Information Trees (DIT)
- Processes LDAP SearchResultEntry messages
- Supports custom organisational units
- Demonstrates directory-based data retrieval

---

# Networking Concepts

This project demonstrates several important networking concepts.

## LDAP Protocol

- LDAP operations
- Bind requests
- Search requests
- Search result entries
- LDAP responses

---

## Binary Protocols

- ASN.1 encoding
- BER encoding
- Binary packet construction
- Binary response parsing
- Byte-level processing

---

## TCP Communication

- TCP sockets
- Client-server communication
- Stream processing
- Packet transmission
- Network connections

---

# Technologies Used

## Core Technologies

- Java
- LDAP
- TCP Sockets
- ASN.1
- BER Encoding

## Java Packages

- java.net
- java.io
- java.util

---

# System Architecture

```text
                User
                  │
                  ▼
      LDAP Asset Directory Client
                  │
                  │ TCP Connection (Port 389)
                  ▼
            LDAP Server
                  │
                  ▼
     Directory Information Tree
                  │
                  ▼
          Asset Information
                  │
                  ▼
          LDAP Response Packet
                  │
                  ▼
      Java Response Parser
                  │
                  ▼
         Display Search Results
```

---

# Repository Structure

```text
ldap-asset-directory-client/

│
├── src/
│   ├── LDAPClient.java
│   ├── BEREncoder.java
│   ├── BERDecoder.java
│   ├── PacketBuilder.java
│   ├── ResponseParser.java
│   └── Utilities.java
│
├── docs/
│   ├── Assignment Specification.pdf
│   ├── LDAP Tree.pdf
│   ├── Screenshots/
│   └── Design Notes.pdf
│
├── screenshots/
│
├── README.md
│
└── LICENSE
```

---

# How It Works

When the application starts, it establishes a TCP connection to an LDAP server listening on the standard LDAP port.

The client constructs an LDAP search request manually using byte-level operations before transmitting the request over the socket.

The LDAP server processes the request, searches the Directory Information Tree (DIT), and returns a binary response.

The client then decodes the returned LDAP packet, extracts the requested information, and displays the asset's maximum speed.

---

# Example Workflow

### Step 1

Launch the client.

```bash
java LDAPClient
```

---

### Step 2

Enter an asset name.

```
Boeing 747
```

---

### Step 3

The client builds an LDAP SearchRequest packet.

---

### Step 4

The packet is transmitted to the LDAP server over TCP.

---

### Step 5

The server searches the directory.

---

### Step 6

The client decodes the LDAP response.

---

### Step 7

The result is displayed.

```
Asset: Boeing 747

Maximum Speed:
988 km/h
```

---

# Skills Demonstrated

This project demonstrates experience with:

- Java networking
- Socket programming
- LDAP protocol implementation
- Binary protocol analysis
- ASN.1 encoding
- BER decoding
- Client-server architecture
- Packet construction
- Byte-level processing
- Directory services

---

# Running the Project

## Clone the Repository

```bash
git clone https://github.com/Lwandle-Chauke/ldap-asset-directory-client.git
```

Compile the application.

```bash
javac *.java
```

Ensure an LDAP server is running.

For example:

- OpenLDAP
- phpLDAPadmin
- Apache Directory Server

Run the client.

```bash
java LDAPClient
```

Enter an asset name when prompted.

---

# Documentation

The repository includes documentation covering:

- Assignment specification
- LDAP architecture
- Directory Information Tree
- BER encoding
- ASN.1 structures
- Packet format
- Screenshots
- Testing

---

# Screenshots

Example screenshots include:

- LDAP directory structure
- Search request
- Client console
- Successful asset lookup
- Directory entries

---

# Learning Outcomes

This project significantly strengthened my understanding of:

- LDAP directory services
- Binary network protocols
- ASN.1 encoding
- BER packet structures
- TCP socket programming
- Directory Information Trees
- Client-server communication
- Byte-level protocol implementation

Building an LDAP client without using existing libraries provided valuable insight into how enterprise directory services communicate across networks.

---

# Future Improvements

Potential enhancements include:

- Secure LDAP (LDAPS)
- TLS encryption
- Authentication support
- Graphical user interface
- Multiple search filters
- CRUD operations
- Asynchronous communication
- Multi-threaded requests
- Directory management tools
- Docker deployment

---

# About Me

I'm **Lwandle Chauke**, a Computer Science graduate with a growing interest in:

- Software Engineering
- Computer Networks
- Cybersecurity
- Application Security
- Backend Development
- Identity and Access Management

I enjoy building networking applications from the ground up to better understand the protocols that power modern enterprise systems.

**GitHub**

https://github.com/Lwandle-Chauke

---

<div align="center">

If you found this project interesting, feel free to star the repository.

</div>
