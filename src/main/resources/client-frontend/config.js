// Configuraciones para diferentes entornos

// Configuración de desarrollo
const DEV_CONFIG = {
    apiBaseUrl: 'http://localhost:8080/api/products/async',
    syncApiBaseUrl: 'http://localhost:8080/api/products',
    adminApiUrl: 'http://localhost:8080/api/admin/worker',
    pollingInterval: 1000, // 1 segundo
};

// Configuración de pruebas
const TEST_CONFIG = {
    apiBaseUrl: 'http://test-server/api/products/async',
    syncApiBaseUrl: 'http://test-server/api/products',
    adminApiUrl: 'http://test-server/api/admin/worker',
    pollingInterval: 2000, // 2 segundos
};

// Configuración de producción
const PROD_CONFIG = {
    apiBaseUrl: 'https://api.example.com/api/products/async',
    syncApiBaseUrl: 'https://api.example.com/api/products',
    adminApiUrl: 'https://api.example.com/api/admin/worker',
    pollingInterval: 3000, // 3 segundos
};

// Determinar qué configuración usar basado en la URL actual
let CURRENT_CONFIG = DEV_CONFIG;

if (window.location.hostname.includes('test')) {
    CURRENT_CONFIG = TEST_CONFIG;
} else if (window.location.hostname.includes('example.com')) {
    CURRENT_CONFIG = PROD_CONFIG;
}

// También se puede forzar un entorno específico descomentando una de estas líneas:
// CURRENT_CONFIG = DEV_CONFIG;
// CURRENT_CONFIG = TEST_CONFIG;
// CURRENT_CONFIG = PROD_CONFIG;