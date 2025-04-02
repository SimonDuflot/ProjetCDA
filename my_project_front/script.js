const fileInput = document.getElementById('resumeFile');
const uploadButton = document.getElementById('uploadButton');
const metadataDisplay = document.getElementById('metadataDisplay');
const modal = document.getElementById('uploadModal');
const modalLog = document.getElementById('modal-log-content');
const closeModalBtn = document.querySelector('.close-modal');
const cancelUploadBtn = document.getElementById('cancelUpload');

let currentUpload = null;

// Open modal when upload button is clicked
uploadButton.addEventListener('click', () => {
    const file = fileInput.files[0];
    if (file) {
        // Clear previous logs
        modalLog.innerHTML = '';
        
        // Show the modal
        modal.style.display = 'block';
        
        // Add initial log entry
        addLogEntry(`Starting upload of "${file.name}" (${formatFileSize(file.size)})`, 'info');
        
        // Start the upload process
        sendBlobToBackend(file);
    } else {
        alert("Please select a file.");
    }
});

// Close modal when the X is clicked
closeModalBtn.addEventListener('click', () => {
    modal.style.display = 'none';
});

// Close modal when clicking outside the modal content
window.addEventListener('click', (event) => {
    if (event.target === modal) {
        modal.style.display = 'none';
    }
});

// Cancel upload button
cancelUploadBtn.addEventListener('click', () => {
    if (currentUpload) {
        currentUpload.abort();
        addLogEntry('Upload cancelled by user', 'error');
    }
    // Don't close the modal immediately so user can see the cancellation message
});

function addLogEntry(message, type = 'info') {
    const entry = document.createElement('div');
    entry.className = `log-entry log-${type}`;
    
    const timestamp = new Date().toLocaleTimeString();
    entry.textContent = `[${timestamp}] ${message}`;
    
    modalLog.appendChild(entry);
    
    // Scroll to bottom
    modalLog.scrollTop = modalLog.scrollHeight;
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' bytes';
    else if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    else return (bytes / 1048576).toFixed(1) + ' MB';
}

function sendBlobToBackend(file) {
    const formData = new FormData();
    formData.append('file', file);
    
    // Create an AbortController to be able to cancel the fetch
    const controller = new AbortController();
    currentUpload = controller;
    
    addLogEntry('Preparing file for upload...', 'info');
    
    // Set up progress tracking
    const xhr = new XMLHttpRequest();
    xhr.open('POST', 'http://localhost:8080/api/uploadResumeBlob', true);
    
    xhr.upload.onprogress = (event) => {
        if (event.lengthComputable) {
            const percentComplete = Math.round((event.loaded / event.total) * 100);
            addLogEntry(`Upload progress: ${percentComplete}%`, 'info');
        }
    };
    
    xhr.onload = function() {
        if (xhr.status === 200) {
            try {
                const data = JSON.parse(xhr.responseText);
                addLogEntry('Upload completed successfully!', 'success');
                
                // Display metadata in the main page
                metadataDisplay.innerHTML = '<h3>Resume Metadata</h3>';
                
                for (const key in data) {
                    const metadataItem = document.createElement('p');
                    metadataItem.textContent = `${key}: ${data[key]}`;
                    metadataDisplay.appendChild(metadataItem);
                }
                
                // Add completion message to log
                addLogEntry('Metadata extracted and displayed', 'success');
                
                // Enable close button or auto-close after delay
                setTimeout(() => {
                    addLogEntry('You can now close this window', 'info');
                }, 2000);
            } catch (error) {
                addLogEntry(`Error parsing server response: ${error.message}`, 'error');
            }
        } else {
            addLogEntry(`Server responded with error code: ${xhr.status}`, 'error');
        }
        
        currentUpload = null;
    };
    
    xhr.onerror = function() {
        addLogEntry('Network error occurred during upload', 'error');
        currentUpload = null;
    };
    
    xhr.onabort = function() {
        addLogEntry('Upload was cancelled', 'error');
        currentUpload = null;
    };
    
    // Start the upload
    addLogEntry('Starting file transfer...', 'info');
    xhr.send(formData);
}