  
import java.io.IOException;  
import java.io.InputStream;  
import java.io.ObjectInput;  
import java.io.ObjectInputStream;  
import java.io.ObjectOutput;  
import java.io.ObjectOutputStream;  
import java.io.OutputStream;  
import java.lang.reflect.InvocationTargetException;  
import java.lang.reflect.Method;  
import java.net.ServerSocket;  
import java.net.Socket;  
import java.util.concurrent.ConcurrentHashMap;  
import java.util.concurrent.ExecutorService;  
import java.util.concurrent.Executors;  
import java.util.concurrent.atomic.AtomicBoolean;  
  
  
public class RPCServer {  
      
    private static final ExecutorService taskPool = Executors.newFixedThreadPool(50) ;  
  
    /** 
     * ����ӿڶ���� 
     * key���ӿ���    value���ӿ�ʵ�� 
     */  
    private static final ConcurrentHashMap<String, Object> serviceTargets =   
        new ConcurrentHashMap<String, Object>() ;  
  
    private static AtomicBoolean run = new AtomicBoolean(false) ;  
      
    /** 
     * ע����� 
     * @param service 
     */  
    public void registService(Object service){  
        Class<?>[] interfaces = service.getClass().getInterfaces() ;  
        if(interfaces == null){  
            throw new IllegalArgumentException("����������ʵ�ֽӿ�");   
        }  
        Class<?> interfacez = interfaces[0] ;  
        String interfaceName = interfacez.getName() ;  
        serviceTargets.put(interfaceName, service) ;  
    }  
  
    /** 
     * ����Server 
     * @param port 
     */  
    public void startServer(final int port){  
        Runnable lifeThread = new Runnable() {  
            @Override  
            public void run() {  
                ServerSocket lifeSocket = null ;  
                Socket client = null ;  
                ServiceTask serviceTask = null ;  
                try {  
                    lifeSocket = new ServerSocket(port) ;  
                    run.set(true) ;  
                    while(run.get()){  
                        client = lifeSocket.accept() ;  
                        serviceTask = new ServiceTask(client);   
                        serviceTask.accept() ;  
                    }  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
        };  
        taskPool.execute(lifeThread) ;    
        System.out.println("���������ɹ�...");  
    }  
      
    public void stopServer(){  
        run.set(false) ;  
        taskPool.shutdown() ;  
    }  
  
    public static final class ServiceTask implements Runnable{  
  
        private Socket client  ;  
          
        public ServiceTask(Socket client){  
            this.client = client ;  
        }  
          
        public void accept(){  
            taskPool.execute(this) ;  
        }  
  
        @Override  
        public void run() {  
            InputStream is = null ;  
            ObjectInput oi = null ;  
            OutputStream os = null ;  
            ObjectOutput oo = null ;  
            try {  
                is = client.getInputStream() ;  
                os = client.getOutputStream() ;  
                oi = new ObjectInputStream(is);  
                String serviceName = oi.readUTF() ;  
                String methodName = oi.readUTF();  
                Class<?>[] paramTypes =  (Class[]) oi.readObject() ;    
                Object[] arguments = (Object[]) oi.readObject() ;  
                System.out.println("serviceName:" + serviceName + " methodName:" + methodName);  
                Object targetService = serviceTargets.get(serviceName) ;  
                if(targetService == null){  
                    throw new ClassNotFoundException(serviceName + "����δ�ҵ���") ;  
                }  
                  
                Method targetMethod = targetService.getClass().getMethod(methodName, paramTypes) ;  
                Object result = targetMethod.invoke(targetService, arguments) ;  
                  
                oo = new ObjectOutputStream(os) ;  
                oo.writeObject(result) ;  
            } catch (IOException e) {  
                e.printStackTrace();  
            } catch (ClassNotFoundException e) {  
                e.printStackTrace();  
            } catch (SecurityException e) {  
                e.printStackTrace();  
            } catch (NoSuchMethodException e) {  
                e.printStackTrace();  
            } catch (IllegalArgumentException e) {  
                e.printStackTrace();  
            } catch (IllegalAccessException e) {  
                e.printStackTrace();  
            } catch (InvocationTargetException e) {  
                e.printStackTrace();  
            }finally{  
                try {  
                    if(oo != null){  
                        oo.close() ;  
                    }  
                    if(os != null){  
                        os.close() ;  
                    }  
                    if(is != null){  
                        is.close() ;  
                    }  
                    if(oi != null){  
                        oi.close() ;  
                    }  
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
        }  
  
    }  
  
  
}  