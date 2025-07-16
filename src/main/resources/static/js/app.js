// Load configurations
async function loadConfigs() {
    const response = await fetch('/api/automation/configs');
    const configs = await response.json();

    const container = document.getElementById('configsList');
    container.innerHTML = '';

    configs.forEach(config => {
        const card = `
            <div class="col-md-4 mb-3">
                <div class="card">
                    <div class="card-body">
                        <h5 class="card-title">${config.name}</h5>
                        <p class="card-text">${config.description || 'No description'}</p>
                        <p class="card-text">
                            <small class="text-muted">
                                Status: ${config.active ? 'Active' : 'Inactive'}
                            </small>
                        </p>
                        <button class="btn btn-sm btn-primary" onclick="runNow(${config.id})">Run Now</button>
                        <button class="btn btn-sm btn-warning" onclick="editConfig(${config.id})">Edit</button>
                        <button class="btn btn-sm btn-info" onclick="cloneConfig(${config.id})">Clone</button>
                        <button class="btn btn-sm btn-secondary" onclick="toggleActive(${config.id})">
                            ${config.active ? 'Deactivate' : 'Activate'}
                        </button>
                        <button class="btn btn-sm btn-danger" onclick="deleteConfig(${config.id})">Delete</button>
                    </div>
                </div>
            </div>
        `;
        container.innerHTML += card;
    });
}

// Pagination state
let currentPage = 0;
let pageSize = 20;
let totalPages = 0;
let totalElements = 0;

// Load history with pagination
async function loadHistory(page = 0) {
    try {
        currentPage = page;
        const response = await fetch(`/api/history?page=${page}&size=${pageSize}`);
        const data = await response.json();

        const tbody = document.getElementById('historyTable');
        tbody.innerHTML = '';

        // Debug: log first result to check date format
        if (data.content.length > 0) {
            console.log('Sample result:', data.content[0]);
            console.log('Start time:', data.content[0].startTime);
        }

        // Update pagination info
        totalPages = data.totalPages;
        totalElements = data.totalElements;
        updatePaginationInfo(data);

        data.content.forEach(result => {
            let duration = 'Running...';
            let startTimeStr = 'N/A';

            if (result.startTime) {
                // Handle different date formats
                let startTime;
                if (Array.isArray(result.startTime)) {
                    // Handle array format [year, month, day, hour, minute, second, nano]
                    const [year, month, day, hour, minute, second] = result.startTime;
                    startTime = new Date(year, month - 1, day, hour, minute, second);
                } else {
                    // Handle ISO string format
                    startTime = new Date(result.startTime);
                }

                if (!isNaN(startTime.getTime())) {
                    startTimeStr = formatDateTime(startTime);

                    if (result.endTime) {
                        let endTime;
                        if (Array.isArray(result.endTime)) {
                            const [year, month, day, hour, minute, second] = result.endTime;
                            endTime = new Date(year, month - 1, day, hour, minute, second);
                        } else {
                            endTime = new Date(result.endTime);
                        }

                        if (!isNaN(endTime.getTime())) {
                            const durationMs = endTime.getTime() - startTime.getTime();
                            duration = formatDuration(durationMs);
                        }
                    }
                }
            }

            const row = `
                <tr>
                    <td>${result.configName || 'Unknown'}</td>
                    <td>
                        <span class="badge bg-${getStatusColor(result.status)}">
                            ${result.status}
                        </span>
                    </td>
                    <td>${startTimeStr}</td>
                    <td>${duration}</td>
                    <td>
                        <button class="btn btn-sm btn-info" onclick="viewDetails(${result.id})">Details</button>
                    </td>
                </tr>
            `;
            tbody.innerHTML += row;
        });

        // Update pagination controls
        updatePaginationControls();

    } catch (error) {
        console.error('Error loading history:', error);
        const tbody = document.getElementById('historyTable');
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Error loading history</td></tr>';
    }
}

// Update pagination info text
function updatePaginationInfo(data) {
    const start = data.number * data.size + 1;
    const end = Math.min((data.number + 1) * data.size, data.totalElements);
    const infoText = `Showing ${start} to ${end} of ${data.totalElements} entries`;
    document.getElementById('historyInfo').textContent = infoText;
}

// Update pagination controls
function updatePaginationControls() {
    const pagination = document.getElementById('historyPagination');
    pagination.innerHTML = '';

    // Previous button
    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${currentPage === 0 ? 'disabled' : ''}`;
    prevLi.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); loadHistory(${currentPage - 1})">Previous</a>`;
    pagination.appendChild(prevLi);

    // Page numbers
    const maxVisiblePages = 5;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);

    if (endPage - startPage < maxVisiblePages - 1) {
        startPage = Math.max(0, endPage - maxVisiblePages + 1);
    }

    // First page
    if (startPage > 0) {
        const firstLi = document.createElement('li');
        firstLi.className = 'page-item';
        firstLi.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); loadHistory(0)">1</a>`;
        pagination.appendChild(firstLi);

        if (startPage > 1) {
            const ellipsisLi = document.createElement('li');
            ellipsisLi.className = 'page-item disabled';
            ellipsisLi.innerHTML = '<span class="page-link">...</span>';
            pagination.appendChild(ellipsisLi);
        }
    }

    // Page numbers
    for (let i = startPage; i <= endPage; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === currentPage ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); loadHistory(${i})">${i + 1}</a>`;
        pagination.appendChild(li);
    }

    // Last page
    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            const ellipsisLi = document.createElement('li');
            ellipsisLi.className = 'page-item disabled';
            ellipsisLi.innerHTML = '<span class="page-link">...</span>';
            pagination.appendChild(ellipsisLi);
        }

        const lastLi = document.createElement('li');
        lastLi.className = 'page-item';
        lastLi.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); loadHistory(${totalPages - 1})">${totalPages}</a>`;
        pagination.appendChild(lastLi);
    }

    // Next button
    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}`;
    nextLi.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); loadHistory(${currentPage + 1})">Next</a>`;
    pagination.appendChild(nextLi);
}

// Change page size
function changePageSize() {
    pageSize = parseInt(document.getElementById('pageSize').value);
    currentPage = 0; // Reset to first page
    loadHistory(0);
}

// Format date time for display
function formatDateTime(date) {
    if (!date || isNaN(date.getTime())) {
        return 'N/A';
    }

    const options = {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
    };

    return date.toLocaleDateString('en-US', options);
}

// Format duration from milliseconds
function formatDuration(milliseconds) {
    if (isNaN(milliseconds) || milliseconds < 0) {
        return 'N/A';
    }

    const seconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) {
        return `${hours}h ${minutes % 60}m ${seconds % 60}s`;
    } else if (minutes > 0) {
        return `${minutes}m ${seconds % 60}s`;
    } else {
        return `${seconds}s`;
    }
}

// Helper functions
function getStatusColor(status) {
    switch(status) {
        case 'SUCCESS': return 'success';
        case 'FAILED': return 'danger';
        case 'RUNNING': return 'primary';
        default: return 'secondary';
    }
}

// Update pagination info text
function updatePaginationInfo(data) {
    const start = data.totalElements > 0 ? (data.number * data.size + 1) : 0;
    const end = Math.min((data.number + 1) * data.size, data.totalElements);
    const infoText = `Showing ${start} to ${end} of ${data.totalElements} entries`;
    document.getElementById('historyInfo').textContent = infoText;
}

// Update pagination controls
function updatePaginationControls() {
    const pagination = document.getElementById('historyPagination');
    pagination.innerHTML = '';

    if (totalPages <= 1) {
        return; // No pagination needed
    }

    // Previous button
    const prevLi = document.createElement('li');
    prevLi.className = `page-item ${currentPage === 0 ? 'disabled' : ''}`;
    if (currentPage > 0) {
        prevLi.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); loadHistory(${currentPage - 1})">Previous</a>`;
    } else {
        prevLi.innerHTML = `<span class="page-link">Previous</span>`;
    }
    pagination.appendChild(prevLi);

    // Page numbers
    const maxVisiblePages = 5;
    let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);

    if (endPage - startPage < maxVisiblePages - 1) {
        startPage = Math.max(0, endPage - maxVisiblePages + 1);
    }

    // First page
    if (startPage > 0) {
        const firstLi = document.createElement('li');
        firstLi.className = 'page-item';
        firstLi.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); loadHistory(0)">1</a>`;
        pagination.appendChild(firstLi);

        if (startPage > 1) {
            const ellipsisLi = document.createElement('li');
            ellipsisLi.className = 'page-item disabled';
            ellipsisLi.innerHTML = '<span class="page-link">...</span>';
            pagination.appendChild(ellipsisLi);
        }
    }

    // Page numbers
    for (let i = startPage; i <= endPage; i++) {
        const li = document.createElement('li');
        li.className = `page-item ${i === currentPage ? 'active' : ''}`;
        li.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); loadHistory(${i})">${i + 1}</a>`;
        pagination.appendChild(li);
    }

    // Last page
    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            const ellipsisLi = document.createElement('li');
            ellipsisLi.className = 'page-item disabled';
            ellipsisLi.innerHTML = '<span class="page-link">...</span>';
            pagination.appendChild(ellipsisLi);
        }

        const lastLi = document.createElement('li');
        lastLi.className = 'page-item';
        lastLi.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); loadHistory(${totalPages - 1})">${totalPages}</a>`;
        pagination.appendChild(lastLi);
    }

    // Next button
    const nextLi = document.createElement('li');
    nextLi.className = `page-item ${currentPage >= totalPages - 1 ? 'disabled' : ''}`;
    if (currentPage < totalPages - 1) {
        nextLi.innerHTML = `<a class="page-link" href="#" onclick="event.preventDefault(); loadHistory(${currentPage + 1})">Next</a>`;
    } else {
        nextLi.innerHTML = `<span class="page-link">Next</span>`;
    }
    pagination.appendChild(nextLi);
}

// Load history with pagination and filters
async function loadHistory(page = 0) {
    try {
        currentPage = page;

        // Build query parameters
        let queryParams = `page=${page}&size=${pageSize}`;

        const statusFilter = document.getElementById('statusFilter');
        if (statusFilter && statusFilter.value) {
            queryParams += `&status=${statusFilter.value}`;
        }

        const configFilter = document.getElementById('configFilter');
        if (configFilter && configFilter.value) {
            queryParams += `&configId=${configFilter.value}`;
        }

        console.log('Loading history with params:', queryParams);

        const response = await fetch(`/api/history?${queryParams}`);
        const data = await response.json();

        console.log('History data:', data);

        const tbody = document.getElementById('historyTable');
        tbody.innerHTML = '';

        // Update pagination info
        totalPages = data.totalPages || 0;
        totalElements = data.totalElements || 0;
        updatePaginationInfo(data);

        if (!data.content || data.content.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">No results found</td></tr>';
        } else {
            data.content.forEach(result => {
                let duration = 'Running...';
                let startTimeStr = 'N/A';

                if (result.startTime) {
                    // Handle different date formats
                    let startTime;
                    if (Array.isArray(result.startTime)) {
                        // Handle array format [year, month, day, hour, minute, second, nano]
                        const [year, month, day, hour, minute, second] = result.startTime;
                        startTime = new Date(year, month - 1, day, hour, minute, second);
                    } else {
                        // Handle ISO string format
                        startTime = new Date(result.startTime);
                    }

                    if (!isNaN(startTime.getTime())) {
                        startTimeStr = formatDateTime(startTime);

                        if (result.endTime) {
                            let endTime;
                            if (Array.isArray(result.endTime)) {
                                const [year, month, day, hour, minute, second] = result.endTime;
                                endTime = new Date(year, month - 1, day, hour, minute, second);
                            } else {
                                endTime = new Date(result.endTime);
                            }

                            if (!isNaN(endTime.getTime())) {
                                const durationMs = endTime.getTime() - startTime.getTime();
                                duration = formatDuration(durationMs);
                            }
                        }
                    }
                }

                const row = `
                    <tr>
                        <td>${result.configName || 'Unknown'}</td>
                        <td>
                            <span class="badge bg-${getStatusColor(result.status)}">
                                ${result.status}
                            </span>
                        </td>
                        <td>${startTimeStr}</td>
                        <td>${duration}</td>
                        <td>
                            <button class="btn btn-sm btn-info" onclick="viewDetails(${result.id})">Details</button>
                        </td>
                    </tr>
                `;
                tbody.innerHTML += row;
            });
        }

        // Update pagination controls
        updatePaginationControls();

    } catch (error) {
        console.error('Error loading history:', error);
        const tbody = document.getElementById('historyTable');
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Error loading history: ' + error.message + '</td></tr>';
    }
}

// Change page size
function changePageSize() {
    const pageSizeElement = document.getElementById('pageSize');
    if (pageSizeElement) {
        pageSize = parseInt(pageSizeElement.value);
        currentPage = 0; // Reset to first page
        loadHistory(0);
    }
}

// Apply filters
function applyFilters() {
    currentPage = 0; // Reset to first page when filtering
    loadHistory(0);
}

// Populate config filter dropdown
async function populateConfigFilter() {
    try {
        const response = await fetch('/api/automation/configs');
        const configs = await response.json();

        const select = document.getElementById('configFilter');
        if (select) {
            select.innerHTML = '<option value="">All Configurations</option>';

            configs.forEach(config => {
                const option = document.createElement('option');
                option.value = config.id;
                option.textContent = config.name;
                select.appendChild(option);
            });
        }
    } catch (error) {
        console.error('Error loading configs for filter:', error);
    }
}

async function viewDetails(resultId) {
    try {
        const response = await fetch(`/api/history/${resultId}`);
        if (!response.ok) throw new Error('Failed to fetch details');

        const result = await response.json();

        let screenshotsHtml = '';
        if (result.screenshotPaths && result.screenshotPaths.length > 0) {
            screenshotsHtml = '<h6>Screenshots:</h6><div class="row">';
            result.screenshotPaths.forEach((path, index) => {
                screenshotsHtml += `
                    <div class="col-md-4 mb-3">
                        <img src="/api/history/${resultId}/screenshot/${index}" 
                             class="img-fluid img-thumbnail" 
                             alt="Screenshot ${index + 1}"
                             style="cursor: pointer;"
                             onclick="window.open('/api/history/${resultId}/screenshot/${index}', '_blank')">
                        <small class="text-muted d-block mt-1">Screenshot ${index + 1}</small>
                    </div>
                `;
            });
            screenshotsHtml += '</div>';
        }

        let duration = 'N/A';
        let startTimeStr = 'N/A';
        let endTimeStr = 'N/A';

        if (result.startTime) {
            const startTime = new Date(result.startTime);
            startTimeStr = formatDateTime(startTime);

            if (result.endTime) {
                const endTime = new Date(result.endTime);
                endTimeStr = formatDateTime(endTime);
                const durationMs = endTime.getTime() - startTime.getTime();
                duration = formatDuration(durationMs);
            }
        }

        const detailsHtml = `
            <div class="row mb-4">
                <div class="col-md-6">
                    <h6>Configuration:</h6>
                    <p><strong>Name:</strong> ${result.configName}</p>
                    <p><strong>Description:</strong> ${result.configDescription || 'N/A'}</p>
                </div>
                <div class="col-md-6">
                    <h6>Execution Info:</h6>
                    <p><strong>Status:</strong> 
                        <span class="badge bg-${getStatusColor(result.status)}">${result.status}</span>
                    </p>
                    <p><strong>Start Time:</strong> ${startTimeStr}</p>
                    <p><strong>End Time:</strong> ${endTimeStr}</p>
                    <p><strong>Duration:</strong> ${duration}</p>
                </div>
            </div>
            
            ${result.errorMessage ? `
                <div class="alert alert-danger">
                    <h6>Error Message:</h6>
                    <pre class="mb-0">${escapeHtml(result.errorMessage)}</pre>
                </div>
            ` : ''}
            
            <div class="mb-4">
                <h6>Execution Logs:</h6>
                <div class="bg-light p-3 rounded" style="max-height: 300px; overflow-y: auto;">
                    <pre class="mb-0">${escapeHtml(result.logs || 'No logs available')}</pre>
                </div>
            </div>
            
            ${screenshotsHtml}
            
            <div class="mb-4">
                <h6>Automation Steps:</h6>
                <div class="table-responsive">
                    <table class="table table-sm table-bordered">
                        <thead>
                            <tr>
                                <th>Order</th>
                                <th>Type</th>
                                <th>Selector</th>
                                <th>Value</th>
                                <th>Wait</th>
                                <th>Screenshot</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${result.steps ? result.steps.map(step => `
                                <tr>
                                    <td>${step.order}</td>
                                    <td>${step.type}</td>
                                    <td><code>${step.selector || '-'}</code></td>
                                    <td>${step.value || '-'}</td>
                                    <td>${step.waitSeconds}s</td>
                                    <td>${step.captureScreenshot ? '✓' : '-'}</td>
                                </tr>
                            `).join('') : '<tr><td colspan="6">No steps available</td></tr>'}
                        </tbody>
                    </table>
                </div>
            </div>
        `;

        document.getElementById('detailsContent').innerHTML = detailsHtml;
        const modal = new bootstrap.Modal(document.getElementById('detailsModal'));
        modal.show();

    } catch (error) {
        alert('Failed to load details: ' + error.message);
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Edit configuration
async function editConfig(configId) {
    try {
        const response = await fetch(`/api/automation/configs/${configId}`);
        if (!response.ok) throw new Error('Failed to fetch configuration');

        const config = await response.json();

        // Set form to edit mode
        document.getElementById('formTitle').textContent = 'Edit Configuration';
        document.getElementById('submitButton').textContent = 'Update Configuration';
        document.getElementById('cancelButton').style.display = 'inline-block';
        document.getElementById('configId').value = configId;

        // Fill basic fields
        document.getElementById('configName').value = config.name;
        document.getElementById('configDescription').value = config.description || '';
        document.getElementById('configActive').checked = config.active;

        // Clear and rebuild steps
        document.getElementById('stepsContainer').innerHTML = '';
        stepCount = 0;

        if (config.steps && config.steps.length > 0) {
            // Sort steps by order
            config.steps.sort((a, b) => a.order - b.order);

            config.steps.forEach(step => {
                addStepWithData(step);
            });
        }

        // Set schedule
        if (config.schedule) {
            document.getElementById('scheduleType').value = config.schedule.type;
            updateScheduleFields();

            // Fill schedule fields
            switch (config.schedule.type) {
                case 'ONCE':
                    if (config.schedule.runOnceAt) {
                        // Convert to datetime-local format
                        const date = new Date(config.schedule.runOnceAt);
                        document.getElementById('runOnceAt').value = date.toISOString().slice(0, 16);
                    }
                    break;
                case 'INTERVAL':
                    document.getElementById('intervalMinutes').value = config.schedule.intervalMinutes || '';
                    break;
                case 'CRON':
                    document.getElementById('cronExpression').value = config.schedule.cronExpression || '';
                    break;
            }
        } else {
            document.getElementById('scheduleType').value = '';
            updateScheduleFields();
        }

        // Scroll to form
        document.getElementById('create').scrollIntoView({ behavior: 'smooth' });

    } catch (error) {
        alert('Error loading configuration: ' + error.message);
    }
}

// Add step with existing data
function addStepWithData(stepData) {
    const container = document.getElementById('stepsContainer');
    const stepDiv = document.createElement('div');
    stepDiv.className = 'card mb-2';
    stepDiv.id = `step-${stepCount}`;

    stepDiv.innerHTML = `
        <div class="card-body">
            <div class="row">
                <div class="col-md-3">
                    <select class="form-select step-type" onchange="updateStepFields(${stepCount})">
                        <option value="NAVIGATE" ${stepData.type === 'NAVIGATE' ? 'selected' : ''}>Navigate</option>
                        <option value="CLICK" ${stepData.type === 'CLICK' ? 'selected' : ''}>Click</option>
                        <option value="INPUT" ${stepData.type === 'INPUT' ? 'selected' : ''}>Input</option>
                        <option value="PASSWORD_INPUT" ${stepData.type === 'PASSWORD_INPUT' ? 'selected' : ''}>Password Input</option>
                        <option value="WAIT" ${stepData.type === 'WAIT' ? 'selected' : ''}>Wait</option>
                        <option value="SCREENSHOT" ${stepData.type === 'SCREENSHOT' ? 'selected' : ''}>Screenshot</option>
                        <option value="SCROLL" ${stepData.type === 'SCROLL' ? 'selected' : ''}>Scroll</option>
                        <option value="SELECT" ${stepData.type === 'SELECT' ? 'selected' : ''}>Select</option>
                    </select>
                </div>
                <div class="col-md-8" id="stepFields-${stepCount}">
                    <!-- Fields will be populated by updateStepFields -->
                </div>
                <div class="col-md-1">
                    <button class="btn btn-sm btn-danger" onclick="removeStep(${stepCount})">X</button>
                </div>
            </div>
            <div class="row mt-2">
                <div class="col-md-6">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="captureScreenshot-${stepCount}" 
                               data-field="captureScreenshot" ${stepData.captureScreenshot ? 'checked' : ''}>
                        <label class="form-check-label" for="captureScreenshot-${stepCount}">
                            Capture screenshot after this step
                        </label>
                    </div>
                </div>
                <div class="col-md-3">
                    <input type="number" class="form-control form-control-sm" placeholder="Wait after (seconds)" 
                           data-field="waitSeconds" min="0" value="${stepData.waitSeconds || 0}">
                </div>
                <div class="col-md-3">
                    <input type="text" class="form-control form-control-sm" placeholder="Screenshot selector (optional)" 
                           data-field="captureSelector" value="${stepData.captureSelector || ''}">
                </div>
            </div>
        </div>
    `;

    container.appendChild(stepDiv);

    // Update fields for this step type
    updateStepFieldsWithData(stepCount, stepData);

    stepCount++;
}

// Update step fields with existing data
function updateStepFieldsWithData(stepId, stepData) {
    const stepDiv = document.getElementById(`step-${stepId}`);
    const stepType = stepData.type;
    const fieldsContainer = document.getElementById(`stepFields-${stepId}`);

    let fields = '';
    switch(stepType) {
        case 'NAVIGATE':
            fields = `<input type="text" class="form-control" placeholder="URL (e.g., https://example.com or example.com)" 
                           data-field="value" value="${stepData.value || ''}" required>`;
            break;
        case 'CLICK':
            fields = `<input type="text" class="form-control" placeholder="CSS Selector (e.g., #submit-button, .btn-primary)" 
                           data-field="selector" value="${stepData.selector || ''}" required>`;
            break;
        case 'INPUT':
            fields = `
                <input type="text" class="form-control mb-2" placeholder="CSS Selector (e.g., #username, input[name='email'])" 
                       data-field="selector" value="${stepData.selector || ''}" required>
                <input type="text" class="form-control" placeholder="Text to input" 
                       data-field="value" value="${stepData.value || ''}">
            `;
            break;
        case 'PASSWORD_INPUT':
            fields = `
                <input type="text" class="form-control mb-2" placeholder="CSS Selector (e.g., #password, input[type='password'])" 
                       data-field="selector" value="${stepData.selector || ''}" required>
                <input type="password" class="form-control" placeholder="Password to input" 
                       data-field="value" value="${stepData.value || ''}">
            `;
            break;
        case 'WAIT':
            // For WAIT step, we don't need additional fields - the wait time is in the common area
            fields = '<div class="alert alert-info mb-0"><i class="bi bi-info-circle"></i> For WAIT steps, use the "Wait after step" field below to set the duration.</div>';
            break;
        case 'SCREENSHOT':
            fields = `<input type="text" class="form-control" placeholder="CSS Selector for specific area (optional, leave empty for full page)" 
                           data-field="captureSelector" value="${stepData.captureSelector || ''}">`;
            break;
        case 'SCROLL':
            fields = `<input type="number" class="form-control" placeholder="Scroll position in pixels (e.g., 500)" 
                           data-field="value" value="${stepData.value || 0}">`;
            break;
        case 'SELECT':
            fields = `
                <input type="text" class="form-control mb-2" placeholder="CSS Selector (e.g., #country-select)" 
                       data-field="selector" value="${stepData.selector || ''}" required>
                <input type="text" class="form-control" placeholder="Option value to select" 
                       data-field="value" value="${stepData.value || ''}" required>
            `;
            break;
    }
    fieldsContainer.innerHTML = fields;
}

// Track form changes
let formChanged = false;

// Mark form as changed when inputs are modified
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('configForm');
    form.addEventListener('input', () => {
        formChanged = true;
    });
    form.addEventListener('change', () => {
        formChanged = true;
    });
});

// Cancel edit mode
function cancelEdit() {
    if (formChanged && !confirm('You have unsaved changes. Are you sure you want to cancel?')) {
        return;
    }

    document.getElementById('formTitle').textContent = 'Create New Configuration';
    document.getElementById('submitButton').textContent = 'Create Configuration';
    document.getElementById('cancelButton').style.display = 'none';
    document.getElementById('configId').value = '';
    document.getElementById('configForm').reset();
    document.getElementById('stepsContainer').innerHTML = '';
    document.getElementById('scheduleFields').innerHTML = '';
    stepCount = 0;
    formChanged = false;
}
function getStatusColor(status) {
    switch(status) {
        case 'SUCCESS': return 'success';
        case 'FAILED': return 'danger';
        case 'RUNNING': return 'primary';
        default: return 'secondary';
    }
}

async function runNow(configId) {
    const response = await fetch(`/api/automation/configs/${configId}/run`, { method: 'POST' });
    if (response.ok) {
        alert('Automation started!');
        loadHistory();
    }
}

async function toggleActive(configId) {
    const response = await fetch(`/api/automation/configs/${configId}/toggle`, { method: 'POST' });
    if (response.ok) {
        loadConfigs();
    }
}

async function deleteConfig(configId) {
    if (!confirm('Are you sure you want to delete this configuration?')) {
        return;
    }

    try {
        // First try to delete without force
        let response = await fetch(`/api/automation/configs/${configId}`, { method: 'DELETE' });

        if (response.status === 409) {
            // Conflict - has results
            const data = await response.json();
            const forceDelete = confirm(
                `${data.message}\n\n` +
                `Do you want to delete the configuration and all ${data.resultCount} execution results?`
            );

            if (forceDelete) {
                response = await fetch(`/api/automation/configs/${configId}?force=true`, { method: 'DELETE' });
            } else {
                return;
            }
        }

        if (response.ok) {
            const result = await response.json();
            alert(result.message || 'Configuration deleted successfully');
            loadConfigs();
            loadHistory();
        } else {
            const error = await response.json();
            alert('Failed to delete: ' + (error.message || 'Unknown error'));
        }
    } catch (error) {
        alert('Error deleting configuration: ' + error.message);
    }
}

// Step management
let stepCount = 0;

function addStep() {
    const container = document.getElementById('stepsContainer');
    const stepDiv = document.createElement('div');
    stepDiv.className = 'card mb-2';
    stepDiv.id = `step-${stepCount}`;

    stepDiv.innerHTML = `
        <div class="card-body">
            <div class="row">
                <div class="col-md-3">
                    <select class="form-select step-type" onchange="updateStepFields(${stepCount})">
                        <option value="NAVIGATE">Navigate</option>
                        <option value="CLICK">Click</option>
                        <option value="INPUT">Input</option>
                        <option value="PASSWORD_INPUT">Password Input</option>
                        <option value="WAIT">Wait</option>
                        <option value="SCREENSHOT">Screenshot</option>
                        <option value="SCROLL">Scroll</option>
                        <option value="SELECT">Select</option>
                    </select>
                </div>
                <div class="col-md-8" id="stepFields-${stepCount}">
                    <input type="text" class="form-control" placeholder="URL (e.g., https://example.com or example.com)" data-field="value" required>
                </div>
                <div class="col-md-1">
                    <button class="btn btn-sm btn-danger" onclick="removeStep(${stepCount})">X</button>
                </div>
            </div>
            <div class="row mt-2">
                <div class="col-md-6">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="captureScreenshot-${stepCount}" data-field="captureScreenshot">
                        <label class="form-check-label" for="captureScreenshot-${stepCount}">
                            Capture screenshot after this step
                        </label>
                    </div>
                </div>
                <div class="col-md-3">
                    <label class="form-label text-muted small">Wait after step:</label>
                    <input type="number" class="form-control form-control-sm" placeholder="Wait after (seconds)" data-field="waitSeconds" min="0" value="0">
                </div>
                <div class="col-md-3">
                    <input type="text" class="form-control form-control-sm" placeholder="Screenshot selector (optional)" data-field="captureSelector">
                </div>
            </div>
        </div>
    `;

    container.appendChild(stepDiv);
    stepCount++;
}

function updateStepFields(stepId) {
    const stepDiv = document.getElementById(`step-${stepId}`);
    const stepType = stepDiv.querySelector('.step-type').value;
    const fieldsContainer = document.getElementById(`stepFields-${stepId}`);

    // Save existing values
    const existingValues = {};
    fieldsContainer.querySelectorAll('[data-field]').forEach(input => {
        existingValues[input.dataset.field] = input.value;
    });

    let fields = '';
    let preserveValue = false;

    switch(stepType) {
        case 'NAVIGATE':
            fields = '<input type="text" class="form-control" placeholder="URL (e.g., https://example.com or example.com)" data-field="value" required>';
            preserveValue = existingValues.value;
            break;
        case 'CLICK':
            fields = '<input type="text" class="form-control" placeholder="CSS Selector (e.g., #submit-button, .btn-primary)" data-field="selector" required>';
            break;
        case 'INPUT':
            fields = `
                <input type="text" class="form-control mb-2" placeholder="CSS Selector (e.g., #username, input[name=\'email\'])" data-field="selector" required>
                <input type="text" class="form-control" placeholder="Text to input" data-field="value">
            `;
            break;
        case 'PASSWORD_INPUT':
            fields = `
                <input type="text" class="form-control mb-2" placeholder="CSS Selector (e.g., #password, input[type=\'password\'])" data-field="selector" required>
                <input type="password" class="form-control" placeholder="Password to input" data-field="value">
            `;
            break;
        case 'WAIT':
            // For WAIT step, we don't need additional fields - the wait time is in the common area
            fields = '<div class="alert alert-info mb-0">Configure wait time in seconds below</div>';
            break;
        case 'SCREENSHOT':
            fields = '<input type="text" class="form-control" placeholder="CSS Selector for specific area (optional, leave empty for full page)" data-field="captureSelector">';
            preserveValue = existingValues.captureSelector;
            break;
        case 'SCROLL':
            fields = '<input type="number" class="form-control" placeholder="Scroll position in pixels (e.g., 500)" data-field="value" value="0">';
            preserveValue = existingValues.value;
            break;
        case 'SELECT':
            fields = `
                <input type="text" class="form-control mb-2" placeholder="CSS Selector (e.g., #country-select)" data-field="selector" required>
                <input type="text" class="form-control" placeholder="Option value to select" data-field="value" required>
            `;
            break;
    }

    fieldsContainer.innerHTML = fields;

    // Restore values where applicable
    fieldsContainer.querySelectorAll('[data-field]').forEach(input => {
        const fieldName = input.dataset.field;
        if (existingValues[fieldName] !== undefined) {
            input.value = existingValues[fieldName];
        }
    });
}

function removeStep(stepId) {
    document.getElementById(`step-${stepId}`).remove();
}

// Schedule fields
function updateScheduleFields() {
    const scheduleType = document.getElementById('scheduleType').value;
    const container = document.getElementById('scheduleFields');

    let fields = '';
    switch(scheduleType) {
        case 'ONCE':
            fields = '<input type="datetime-local" class="form-control" id="runOnceAt">';
            break;
        case 'INTERVAL':
            fields = '<input type="number" class="form-control" id="intervalMinutes" placeholder="Minutes">';
            break;
        case 'CRON':
            fields = '<input type="text" class="form-control" id="cronExpression" placeholder="0 0 * * * ?">';
            break;
    }
    container.innerHTML = fields;
}

// Form submission
document.getElementById('configForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    // Check if we're in edit mode
    const configId = document.getElementById('configId').value;
    const isEdit = configId && configId.trim() !== '';

    // Validate form
    const name = document.getElementById('configName').value.trim();
    if (!name) {
        alert('Configuration name is required');
        return;
    }

    const steps = [];
    const stepDivs = document.querySelectorAll('[id^="step-"]');

    if (stepDivs.length === 0) {
        alert('At least one step is required');
        return;
    }

    // Validate and collect steps
    for (let index = 0; index < stepDivs.length; index++) {
        const stepDiv = stepDivs[index];
        const type = stepDiv.querySelector('.step-type').value;
        const step = {
            order: index,
            type: type,
            waitSeconds: 0,
            captureScreenshot: false
        };

        // Collect all fields including checkboxes
        stepDiv.querySelectorAll('[data-field]').forEach(input => {
            if (input.type === 'checkbox') {
                step[input.dataset.field] = input.checked;
            } else if (input.type === 'number') {
                step[input.dataset.field] = parseInt(input.value) || 0;
            } else {
                step[input.dataset.field] = input.value;
            }
        });

        // Validate required fields based on step type
        let error = null;
        switch (type) {
            case 'NAVIGATE':
                if (!step.value || !step.value.trim()) {
                    error = `Step ${index + 1} (Navigate): URL is required`;
                }
                break;
            case 'CLICK':
                if (!step.selector || !step.selector.trim()) {
                    error = `Step ${index + 1} (Click): CSS Selector is required`;
                }
                break;
            case 'INPUT':
                if (!step.selector || !step.selector.trim()) {
                    error = `Step ${index + 1} (Input): CSS Selector is required`;
                }
                break;
            case 'SELECT':
                if (!step.selector || !step.selector.trim()) {
                    error = `Step ${index + 1} (Select): CSS Selector is required`;
                }
                if (!step.value || !step.value.trim()) {
                    error = `Step ${index + 1} (Select): Option value is required`;
                }
                break;
            case 'WAIT':
                // For WAIT step, ensure waitSeconds has a valid value
                if (!step.waitSeconds || step.waitSeconds <= 0) {
                    error = `Step ${index + 1} (Wait): Wait time must be greater than 0`;
                }
                break;
        }

        if (error) {
            alert(error);
            return;
        }

        steps.push(step);
    }

    const scheduleType = document.getElementById('scheduleType').value;
    let schedule = null;
    if (scheduleType) {
        schedule = { type: scheduleType };
        switch(scheduleType) {
            case 'ONCE':
                const runOnceAt = document.getElementById('runOnceAt').value;
                if (!runOnceAt) {
                    alert('Schedule: Run once date/time is required');
                    return;
                }
                schedule.runOnceAt = new Date(runOnceAt).toISOString();
                break;
            case 'INTERVAL':
                const intervalMinutes = parseInt(document.getElementById('intervalMinutes').value);
                if (!intervalMinutes || intervalMinutes <= 0) {
                    alert('Schedule: Interval must be greater than 0');
                    return;
                }
                schedule.intervalMinutes = intervalMinutes;
                break;
            case 'CRON':
                const cronExpression = document.getElementById('cronExpression').value;
                if (!cronExpression || !cronExpression.trim()) {
                    alert('Schedule: Cron expression is required');
                    return;
                }
                schedule.cronExpression = cronExpression;
                break;
        }
    }

    const config = {
        name: name,
        description: document.getElementById('configDescription').value,
        steps: steps,
        schedule: schedule,
        active: document.getElementById('configActive').checked
    };

    try {
        const url = isEdit ? `/api/automation/configs/${configId}` : '/api/automation/configs';
        const method = isEdit ? 'PUT' : 'POST';

        const response = await fetch(url, {
            method: method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        });

        if (response.ok) {
            alert(`Configuration ${isEdit ? 'updated' : 'created'} successfully!`);
            formChanged = false; // Reset change tracking
            cancelEdit(); // Reset form
            loadConfigs();
        } else {
            const error = await response.text();
            alert(`Failed to ${isEdit ? 'update' : 'create'} configuration: ${error}`);
        }
    } catch (error) {
        alert(`Error ${isEdit ? 'updating' : 'creating'} configuration: ${error.message}`);
    }
});

// Wait for DOM to be ready
document.addEventListener('DOMContentLoaded', function() {
    // Initial load
    loadConfigs();
    loadHistory();
    populateConfigFilter();

    // Auto-refresh history every 10 seconds
    setInterval(() => {
        loadHistory(currentPage); // Maintain current page
    }, 10000);
});