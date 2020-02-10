package endexcase.scanmachine.util;

public class Hex {
	private static final String HEXES = "0123456789ABCDEF";
	
	public static String hexBytesToString(byte[] hexBytes){
		StringBuilder hex = new StringBuilder( 2 * hexBytes.length );  
		for ( final byte b : hexBytes ) {  
			hex.append(HEXES.charAt((b & 0xF0) >> 4))  
			.append(HEXES.charAt((b & 0x0F)));  
		}  
  		return hex.toString();  
	}
	
	public static String hexByteToString(byte hexByte){
		StringBuilder hex = new StringBuilder();
		hex.append(HEXES.charAt((hexByte & 0xF0) >> 4))  
		.append(HEXES.charAt((hexByte & 0x0F)));  
		
  		return hex.toString();  
	}
}
