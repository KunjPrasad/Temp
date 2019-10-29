package dataObject;

/**
 * The serialization proxy class for DataClass
 * 
 * @author Kunj
 *
 */
public class DataClassSerDeProxy {
    private String hexVersionStr;
    private String data;

    public DataClassSerDeProxy() {
        this.hexVersionStr = null;
        this.data = null;
    }

    public DataClassSerDeProxy(String hexVersionStr, String data) {
        this.hexVersionStr = hexVersionStr;
        this.data = data;
    }

    public String getHexVersionStr() {
        return hexVersionStr;
    }

    public void setHexVersionStr(String hexVersionStr) {
        this.hexVersionStr = hexVersionStr;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
