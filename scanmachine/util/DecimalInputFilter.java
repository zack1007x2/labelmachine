package endexcase.scanmachine.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.InputFilter;
import android.text.Spanned;
import endexcase.scanmachine.Interface.IOnReceiveCharListener;

public class DecimalInputFilter implements InputFilter {

	private Pattern mPattern;
	private double maxLimit;
	private double minLimit;
	private boolean hasLimit;
	private IOnReceiveCharListener mCharListener;
    public DecimalInputFilter(int digitsBeforeDecimal, int digitsAfterDecimal) {
        mPattern = Pattern.compile("^-?\\d{0," + digitsBeforeDecimal + "}"
        		+ "([\\.](\\d{0," + digitsAfterDecimal +"})?)?$");
        hasLimit = false;
    }
    
    public DecimalInputFilter(double max, double min, int digitsBeforeDecimal, int digitsAfterDecimal) {
        mPattern = Pattern.compile("^-?\\d{0," + digitsBeforeDecimal + "}"
        		+ "([\\.](\\d{0," + digitsAfterDecimal +"})?)?$");
        maxLimit = max;
        minLimit = min;
        hasLimit = true;
    }
    
    public void setOnCharReceiveListener(IOnReceiveCharListener charListener){
    	mCharListener = charListener;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, 
                               int dstart, int dend) {

        String newString =
            dest.toString().substring(0, dstart) + source.toString().substring(start, end) 
            + dest.toString().substring(dend, dest.toString().length());
        	
        Matcher matcher = mPattern.matcher(newString);
        if(mCharListener!=null && newString.length()>1){
        	mCharListener.onReceiveKey(String.valueOf(newString.charAt(newString.length()-1)));
        }
        if(hasLimit){
        	try {
    			double input = Double.parseDouble(dest.toString() + source.toString());
    			if (isInRange(minLimit, maxLimit, input) && matcher.matches())
    				return null;
    		} catch (NumberFormatException nfe) {
    		}
    		return "";
        }else{
        	if (!matcher.matches()) {
                return "";
            }
        }
        return null;
    }
    
    private boolean isInRange(double min, double max, double input) {
		return max > min ? input >= min && input <= max : input >= max
				&& input <= min;
	}
}
