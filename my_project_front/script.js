const fileInput = document.getElementById('resumeFile');
const uploadButton = document.getElementById('uploadButton');
const metadataDisplay = document.getElementById('metadataDisplay');

uploadButton.addEventListener('click', () => {
    const file = fileInput.files[0];
    if (file) {
        sendBlobToBackend(file); // Send the File object (which is a Blob)
    } else {
        alert("Please select a file.");
    }
});

function sendBlobToBackend(file) {
    const formData = new FormData();
    formData.append('file', file); 

    fetch('http://localhost:8080/api/uploadResumeBlob', {
        method: 'POST',
        body: formData 
    })
    .then(response => response.json())
    .then(data => {
        console.log('Success:', data);
        // Handle the backend's response (e.g., display a message)
    })
    .catch(error => {
        console.error('Error:', error);
        // Display an error message
    });
}