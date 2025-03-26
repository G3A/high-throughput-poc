// Inicializar el cliente con la configuración del entorno
const client = new ProductClient(CURRENT_CONFIG);

// Elementos DOM
const statusIndicator = document.getElementById('status-indicator');
const statusText = document.getElementById('status-text');
const getAllBtn = document.getElementById('get-all-btn');
const getPagedBtn = document.getElementById('get-paged-btn');
const searchBtn = document.getElementById('search-btn');
const searchPanel = document.getElementById('search-panel');
const searchInput = document.getElementById('search-input');
const executeSearchBtn = document.getElementById('execute-search-btn');
const paginationPanel = document.getElementById('pagination-panel');
const pageSize = document.getElementById('page-size');
const pageNumber = document.getElementById('page-number');
const executePagedBtn = document.getElementById('execute-paged-btn');
const loading = document.getElementById('loading');
const taskId = document.getElementById('task-id');
const errorContainer = document.getElementById('error-container');
const productsList = document.getElementById('products-list');
const paginationInfo = document.getElementById('pagination-info');
const pageInfo = document.getElementById('page-info');
const refreshStatsBtn = document.getElementById('refresh-stats-btn');
const statsLoading = document.getElementById('stats-loading');

// Estado de la aplicación
let currentPage = 0;
let totalPages = 0;
let currentSize = 10;
let currentAction = null;
let currentSearchTerm = '';
let currentPollingCancel = null;

// Funciones auxiliares
function showLoading(id = '') {
    productsList.innerHTML = '';
    errorContainer.classList.add('hidden');
    loading.classList.remove('hidden');
    statusIndicator.classList.add('loading');
    statusText.textContent = 'Procesando...';

    if (id) {
        taskId.textContent = `ID de tarea: ${id}`;
    } else {
        taskId.textContent = '';
    }
}

function hideLoading() {
    loading.classList.add('hidden');
    statusIndicator.classList.remove('loading');
    statusText.textContent = 'Listo';
}

function showError(message) {
    errorContainer.textContent = `Error: ${message}`;
    errorContainer.classList.remove('hidden');
    statusIndicator.classList.add('error');
    statusText.textContent = 'Error';
}

function resetUI() {
    hideLoading();
    errorContainer.classList.add('hidden');
    statusIndicator.classList.remove('error');
    statusText.textContent = 'Listo';

    // Cancelar cualquier SSE existente
    if (currentPollingCancel) {
        currentPollingCancel();
        currentPollingCancel = null;
    }
}

function displayProducts(products) {
    productsList.innerHTML = '';

    if (!products || products.length === 0) {
        const row = document.createElement('tr');
        row.innerHTML = `<td colspan="5" class="no-data">No se encontraron productos</td>`;
        productsList.appendChild(row);
        return;
    }

    products.forEach(product => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${product.id || '-'}</td>
            <td>${product.name || '-'}</td>
            <td>${product.category || '-'}</td>
            <td>$${product.price?.toFixed(2) || '-'}</td>
            <td>${product.stock || '0'}</td>
        `;
        productsList.appendChild(row);
    });
}

function displayPagedProducts(data) {
    // Para resultados paginados
    if (data.totalPages !== undefined) {
        displayProducts(data.products);

        // Actualizar información de paginación
        currentPage = data.currentPage;
        totalPages = data.totalPages;
        currentSize = data.pageSize;

        pageInfo.textContent = `Página ${currentPage + 1} de ${totalPages} (${data.totalElements} productos en total)`;
        paginationInfo.classList.remove('hidden');

        // Actualizar el campo de número de página
        pageNumber.value = currentPage;
    } else {
        // Para resultados no paginados
        displayProducts(data.products);
        paginationInfo.classList.add('hidden');
    }
}

// Eventos
getAllBtn.addEventListener('click', async () => {
    resetUI();
    searchPanel.classList.add('hidden');
    paginationPanel.classList.add('hidden');

    try {
        showLoading();
        currentAction = 'getAll';

        const response = await client.getAllProducts();

        if (response.pending) {
            showLoading(response.taskId);

            // Usar SSE para obtener actualizaciones en tiempo real
            const eventSource = client.setupSSEListener(
                response.taskId,
                // onProcessed
                (result) => {
                    hideLoading();
                    displayProducts(result.products);
                },
                // onRejected
                () => {
                    hideLoading();
                    showError('La tarea tardó demasiado tiempo y fue rechazada');
                },
                // onError
                (error) => {
                    hideLoading();
                    showError(error.message || 'Error en la conexión SSE');
                }
            );

            // Guardar referencia al eventSource para poder cerrarlo después
            currentPollingCancel = () => {
                if (eventSource) eventSource.close();
            };
        }
    } catch (error) {
        hideLoading();
        showError(error.message);
    }
});

getPagedBtn.addEventListener('click', () => {
    resetUI();
    searchPanel.classList.add('hidden');
    paginationPanel.classList.remove('hidden');
    currentAction = 'getPaged';
});

searchBtn.addEventListener('click', () => {
    resetUI();
    paginationPanel.classList.add('hidden');
    searchPanel.classList.remove('hidden');
    currentAction = 'search';
});

executePagedBtn.addEventListener('click', async () => {
    resetUI();

    try {
        const page = parseInt(pageNumber.value) || 0;
        const size = parseInt(pageSize.value) || 10;

        showLoading();

        const response = await client.getAllProductsPaged(page, size);

        if (response.pending) {
            showLoading(response.taskId);

            // Usar SSE para obtener actualizaciones en tiempo real
            const eventSource = client.setupSSEListener(
                response.taskId,
                // onProcessed
                (result) => {
                    hideLoading();
                    displayPagedProducts(result);
                },
                // onRejected
                () => {
                    hideLoading();
                    showError('La tarea tardó demasiado tiempo y fue rechazada');
                },
                // onError
                (error) => {
                    hideLoading();
                    showError(error.message || 'Error en la conexión SSE');
                }
            );

            // Guardar referencia al eventSource para poder cerrarlo después
            currentPollingCancel = () => {
                if (eventSource) eventSource.close();
            };
        }
    } catch (error) {
        hideLoading();
        showError(error.message);
    }
});

executeSearchBtn.addEventListener('click', async () => {
    resetUI();

    const keyword = searchInput.value.trim();
    if (!keyword) {
        showError('Por favor ingrese un término de búsqueda');
        return;
    }

    currentSearchTerm = keyword;

    try {
        showLoading();

        const response = await client.searchProducts(keyword);

        if (response.pending) {
            showLoading(response.taskId);

            // Usar SSE para obtener actualizaciones en tiempo real
            const eventSource = client.setupSSEListener(
                response.taskId,
                // onProcessed
                (result) => {
                    hideLoading();
                    displayProducts(result.products);
                },
                // onRejected
                () => {
                    hideLoading();
                    showError('La tarea tardó demasiado tiempo y fue rechazada');
                },
                // onError
                (error) => {
                    hideLoading();
                    showError(error.message || 'Error en la conexión SSE');
                }
            );

            // Guardar referencia al eventSource para poder cerrarlo después
            currentPollingCancel = () => {
                if (eventSource) eventSource.close();
            };
        }
    } catch (error) {
        hideLoading();
        showError(error.message);
    }
});

// Estadísticas del worker
async function loadWorkerStats() {
    statsLoading.classList.remove('hidden');

    try {
        const stats = await client.getWorkerStats();

        document.getElementById('tasks-submitted').textContent = stats.totalTasksProcessed || '0';
        document.getElementById('tasks-processed').textContent = stats.tasksSuccessful || '0';
        document.getElementById('tasks-rejected').textContent = stats.tasksRejected || '0';
        document.getElementById('uptime').textContent = `${stats.uptime || '0'} segundos`;
        document.getElementById('avg-processing-time').textContent = `${stats.avgProcessingTimeMs?.toFixed(2) || '0'} ms`;
        document.getElementById('queue-size').textContent = stats.storedResults || '0';

        statsLoading.classList.add('hidden');
    } catch (error) {
        console.error('Error loading worker stats:', error);
        statsLoading.classList.add('hidden');
    }
}

refreshStatsBtn.addEventListener('click', loadWorkerStats);

// Inicialización
function init() {
    console.log('Cliente de productos inicializado');
    statusText.textContent = 'Listo';

    // Cargar estadísticas iniciales
    loadWorkerStats();

    // Configurar intervalo para actualizar estadísticas automáticamente
    setInterval(loadWorkerStats, 10000);
    // Configurar intervalo para actualizar estadísticas automáticamente
    setInterval(loadWorkerStats, 10000); // Actualizar cada 10 segundos
}

// Iniciar la aplicación
init();