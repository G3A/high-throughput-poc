class ProductClient {
    constructor(options = {}) {
        // Valores por defecto
        this.config = {
            apiBaseUrl: 'http://localhost:8080/api/products/async',
            syncApiBaseUrl: 'http://localhost:8080/api/products',
            adminApiUrl: 'http://localhost:8080/api/admin/worker',
            pollingInterval: 1000, // Intervalo para consultar estado de tareas (1 segundo)
            ...options
        };
    }

    async fetchWithWorker(endpoint, params = {}) {
        // Construir la URL con los parámetros
        const url = new URL(`${this.config.apiBaseUrl}${endpoint}`);
        Object.keys(params).forEach(key =>
            url.searchParams.append(key, params[key]));

        try {
            // 1. Enviar la solicitud inicial para encolar la tarea
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                },
                // Incluir credenciales si se necesitan cookies
                // credentials: 'include'
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const data = await response.json();

            if (data.status === 'ACCEPTED') {
                // Devolver inmediatamente el ID de tarea para mostrar en UI
                return {
                    taskId: data.idTask,
                    pending: true
                };
            } else {
                throw new Error(`Unexpected response: ${JSON.stringify(data)}`);
            }
        } catch (error) {
            console.error('Error:', error);
            throw error;
        }
    }

    // Consultar el estado de una tarea
    async getTaskStatus(taskId) {
        try {
            const response = await fetch(`${this.config.apiBaseUrl}/task/${taskId}`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching task status:', error);
            throw error;
        }
    }

    // Configurar un EventSource para suscribirse a actualizaciones SSE
    setupSSEListener(taskId, onProcessed, onRejected, onError) {
        try {
            const eventSource = new EventSource(`${this.config.apiBaseUrl}/subscribe/${taskId}`);

            eventSource.onmessage = (event) => {
                const data = JSON.parse(event.data);

                if (data.status === 'PROCESSED') {
                    onProcessed(data.result);
                    eventSource.close();
                } else if (data.status === 'REJECTED') {
                    onRejected();
                    eventSource.close();
                }
            };

            eventSource.onerror = (error) => {
                onError(error);
                eventSource.close();
            };

            return eventSource;
        } catch (error) {
            onError(error);
            return null;
        }
    }

    // Métodos específicos para cada endpoint
    getAllProducts() {
        return this.fetchWithWorker('');
    }

    getAllProductsPaged(page = 0, size = 20) {
        return this.fetchWithWorker('/paged', { page, size });
    }

    getProductById(id) {
        return this.fetchWithWorker(`/${id}`);
    }

    getProductsByCategory(category) {
        return this.fetchWithWorker(`/category/${category}`);
    }

    getProductsByCategoryPaged(category, page = 0, size = 20) {
        return this.fetchWithWorker(`/category/${category}/paged`, { page, size });
    }

    getProductsByPriceRange(min, max) {
        return this.fetchWithWorker('/price', { min, max });
    }

    getProductsByPriceRangePaged(min, max, page = 0, size = 20) {
        return this.fetchWithWorker('/price/paged', { min, max, page, size });
    }

    getProductsByMinStock(min) {
        return this.fetchWithWorker('/stock', { min });
    }

    getProductsByMinStockPaged(min, page = 0, size = 20) {
        return this.fetchWithWorker('/stock/paged', { min, page, size });
    }

    searchProducts(keyword) {
        return this.fetchWithWorker('/search', { keyword });
    }

    searchProductsPaged(keyword, page = 0, size = 20) {
        return this.fetchWithWorker('/search/paged', { keyword, page, size });
    }

    // Obtener estadísticas del worker
    async getWorkerStats() {
        try {
            const response = await fetch(`${this.config.adminApiUrl}/stats`, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json'
                }
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            return await response.json();
        } catch (error) {
            console.error('Error fetching worker stats:', error);
            throw error;
        }
    }
}