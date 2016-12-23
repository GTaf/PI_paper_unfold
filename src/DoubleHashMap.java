import java.util.HashMap;


public class DoubleHashMap<X,Y> {
    private  HashMap<X,Y> h1;
    private  HashMap<Y,X> h2;

    DoubleHashMap(){
        this.h1 = new HashMap<>();
        this.h2 = new HashMap<>();
    }

    void put(X key, Y value){
        h1.put(key,value);
        h2.put(value,key);
        
    }

    X get(Y key){
        return h2.get(key);
    }

    public Y get2(X key){
        return h1.get(key);
    }
    
    public void values(){
    	System.out.println(h2.values());
    }
    
    public void size(){
    	System.out.println(h1.values().size());
    }
}

