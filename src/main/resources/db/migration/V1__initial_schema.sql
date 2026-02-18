-- V1__initial_schema.sql
-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create documents table
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create document_tags table
CREATE TABLE document_tags (
    document_id BIGINT NOT NULL,
    tag VARCHAR(255) NOT NULL,
    CONSTRAINT fk_document_tags FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, tag)
);

-- Create file_versions table
CREATE TABLE file_versions (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    file_key VARCHAR(255) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_versions_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_file_versions_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes
CREATE INDEX idx_documents_owner ON documents(owner_id);
CREATE INDEX idx_documents_status ON documents(status);
CREATE INDEX idx_documents_created_at ON documents(created_at);
CREATE INDEX idx_file_versions_document ON file_versions(document_id);
CREATE INDEX idx_file_versions_uploaded_at ON file_versions(uploaded_at);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

