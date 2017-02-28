  
  
import java.io.InputStream;  
import java.io.ObjectInput;  
import java.io.ObjectInputStream;  
import java.io.ObjectOutput;  
import java.io.ObjectOutputStream;  
import java.io.OutputStream;  
import java.lang.reflect.InvocationHandler;  
import java.lang.reflect.Method;  
import java.lang.reflect.Proxy;  
import java.net.Socket;  
  
  
  
public class RPCClient {  
  
    /** 
     * ���ݽӿ����͵õ�����Ľӿ�ʵ�� 
     * @param <T> 
     * @param host  RPC������IP 
     * @param port  RPC����˿� 
     * @param serviceInterface  �ӿ����� 
     * @return  ������Ľӿ�ʵ�� 
     */  
    @SuppressWarnings("unchecked")  
    public static <T> T findService(final String host , final int port ,final Class<T> serviceInterface){  
        return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class[]{serviceInterface}, new InvocationHandler() {  
            @SuppressWarnings("resource")
			@Override  
            public Object invoke(final Object proxy, final Method method, final Object[] args)  
            throws Throwable {  
                Socket socket = null ;  
                InputStream is = null ;  
                OutputStream os = null ;  
                ObjectInput oi = null ;  
                ObjectOutput oo = null ;  
                try {  
                    socket = new Socket(host, port) ;  
                    os = socket.getOutputStream() ;  
                    oo = new ObjectOutputStream(os);  
                    oo.writeUTF(serviceInterface.getName()) ;  
                    oo.writeUTF(method.getName()) ;  
                    oo.writeObject(method.getParameterTypes()) ;  
                    oo.writeObject(args);  
  
                    is = socket.getInputStream() ;  
                    oi = new ObjectInputStream(is) ;  
                    return oi.readObject() ;  
                } catch (Exception e) {  
                    System.out.println("���÷����쳣...");  
                    return null ;  
                }finally{  
                    if(is != null){  
                        is.close() ;  
                    }  
                    if(os != null){  
                        is.close() ;  
                    }  
                    if(oi != null){  
                        is.close() ;  
                    }  
                    if(oo != null){  
                        is.close() ;  
                    }  
                    if(socket != null){  
                        is.close() ;  
                    }  
                }  
            }  
        });   
    }  
  
} 