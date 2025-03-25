package co.g3a.high_throughput_poc.worker;

/**
 * Interfaz para procesadores de tareas
 * @param <T> Tipo del objeto de entrada (payload)
 * @param <R> Tipo del resultado
 */
public interface TaskProcessor<T, R> {
    /**
     * Devuelve el tipo de tarea que este procesador maneja
     */
    String getTaskType();
    
    /**
     * Procesa una tarea y devuelve un resultado
     */
    R processTask(T request);
}