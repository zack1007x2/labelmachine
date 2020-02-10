package endexcase.scanmachine.adapter.item;

/**
 * Created by Zack on 2016/1/30.
 */
public class MemoryItem {

    int ItemId;
    String fileName;
    boolean isCheck;

    public MemoryItem(int itemId) {
        ItemId = itemId;
    }

    public int getItemId() {
        return ItemId;
    }

    public void setItemId(int itemId) {
        ItemId = itemId;
    }

    public boolean isCheck() {
        return isCheck;
    }

    public void setIsCheck(boolean isCheck) {
        this.isCheck = isCheck;
    }

    public String getItemFileName() {
        return fileName;
    }

    public void setItemFileName(String fileName) {
        this.fileName = fileName;
    }
}
