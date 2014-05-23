package ace.io.ocr.receiptocr;

import com.googlecode.eyesfree.textdetect.Thresholder;
import com.googlecode.leptonica.android.AdaptiveMap;
import com.googlecode.leptonica.android.Binarize;
import com.googlecode.leptonica.android.Box;
import com.googlecode.leptonica.android.Convert;
import com.googlecode.leptonica.android.Enhance;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.Rotate;
import com.googlecode.leptonica.android.Skew;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

public class MainActivity extends Activity implements OnClickListener {
	Button buttonLoad;
	ImageView imageGrayscale, imageBinary, imageOriginal, imageSegmented;
	TessBaseAPI baseapi;
	EditText editText;
	
	private static final int RESULT_LOAD_IMAGE = 1;
	//private static final int PICK_FROM_GALLERY = 2;
	Bitmap image = null; 
	Pix pix = null;
	String image_source;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		buttonLoad = (Button)findViewById(R.id.button_load);
		buttonLoad.setOnClickListener(this);
		
		imageOriginal = (ImageView)findViewById(R.id.image_original);
		imageGrayscale = (ImageView)findViewById(R.id.image_grayscale);
		imageBinary = (ImageView)findViewById(R.id.image_binary);
		imageSegmented = (ImageView)findViewById(R.id.imageView_segmented);
		
		editText = (EditText)findViewById(R.id.editText1);
		
		baseapi = new TessBaseAPI();
		baseapi.init("/storage/sdcard0/tesseract/", "eng", TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
		case R.id.button_load:
			Intent intent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, RESULT_LOAD_IMAGE);
			break;
		default:break;
		}
	}
	
	protected void onActivityResult(int requestCode, int resultCode, 
		       Intent imageReturnedIntent) {
		    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

		    switch(requestCode) { 
		    case RESULT_LOAD_IMAGE:
		        if(resultCode == RESULT_OK && imageReturnedIntent != null){  
		            Uri selectedImage = imageReturnedIntent.getData();
		            String[] filePathColumn = {MediaStore.Images.Media.DATA};

		            Cursor cursor = getContentResolver().query(
		                               selectedImage, filePathColumn, null, null, null);
		            cursor.moveToFirst();

		            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
		            image_source = cursor.getString(columnIndex);
		            cursor.close();


		            image = getScaledImage(2, BitmapFactory.decodeFile(image_source));
		            imageOriginal.setImageBitmap(image);
		            
		            Pix pix = ReadFile.readBitmap(image);
		            //pix = Rotate.rotate(pix, 90, true, true);

		            Pix pixGray = AdaptiveMap.backgroundNormMorph(Convert.convertTo8(pix));
		            imageGrayscale.setImageBitmap(WriteFile.writeBitmap(pixGray));
		            
		            //pix.recycle();
		            
		            //imageSegmented.setImageBitmap(WriteFile.writeBitmap(Thresholder.edgeAdaptiveThreshold(pixGray)));
		            
		            Pix pix2;// = Binarize.otsuAdaptiveThreshold(Convert.convertTo8(pixGray));
		            
		            pix2 = Binarize.sauvolaBinarizeTiled(Enhance.unsharpMasking(pixGray, 3, 0.7f), 7, 0.2f, 1, 1);
		            
		            imageBinary.setImageBitmap(WriteFile.writeBitmap(pix2));
		            imageSegmented.setImageBitmap(WriteFile.writeBitmap(Binarize.otsuAdaptiveThreshold(pix)));
		            
		         // erode
		            for(int y = 0; y < pix2.getHeight(); y++)
		            {
		            	for(int x = 0; x < pix2.getWidth(); x++)
		            	{
		            		if (pix2.getPixel(x, y) == 1)
		            		{
		                        if (y>0 && pix2.getPixel(x,y-1)==0) pix2.setPixel(x,y-1,2);
		                        if (x>0 && pix2.getPixel(x-1,y)==0) pix2.setPixel(x-1,y,2);
		                        if (x+1<pix2.getWidth() && pix2.getPixel(x+1,y)==0) pix2.setPixel(x+1,y,2);
		                        if (y+1<pix2.getHeight() && pix2.getPixel(x,y+1)==0) pix2.setPixel(x,y+1,2);
		                    }
		            	}
		            }
		            
		            for(int y = 0; y < pix2.getHeight(); y++)
		            {
		            	for(int x = 0; x < pix2.getWidth(); x++)
		            	{
		            		if (pix2.getPixel(x, y) == 2)
		            		{
		            			pix2.setPixel(x,y-1,1);
		            		}
		            	}
		            }
		            
		         // Dilate
		            for(int y = 0; y < pix2.getHeight(); y++)
		            {
		            	for(int x = 0; x < pix2.getWidth(); x++)
		            	{
		            		if (pix2.getPixel(x, y) == 0)
		            		{
		                        if (y>0 && pix2.getPixel(x,y-1)==1) pix2.setPixel(x,y-1,2);
		                        //if (y>1 && pix2.getPixel(x,y-2)==1) pix2.setPixel(x,y-2,2);
		                        
		                        if (x>0 && pix2.getPixel(x-1,y)==1) pix2.setPixel(x-1,y,2);
		                        //if (x>1 && pix2.getPixel(x-2,y)==1) pix2.setPixel(x-2,y,2);
		                        
		                        if (x+1<pix2.getWidth() && pix2.getPixel(x+1,y)==1) pix2.setPixel(x+1,y,2);
		                        //if (x+2<pix2.getWidth() && pix2.getPixel(x+2,y)==1) pix2.setPixel(x+2,y,2);
		                        
		                        if (y+1<pix2.getHeight() && pix2.getPixel(x,y+1)==1) pix2.setPixel(x,y+1,2);
		                        //if (y+2<pix2.getHeight() && pix2.getPixel(x,y+2)==1) pix2.setPixel(x,y+2,2);
		                        
		                        if (y+1<pix2.getHeight() && x+1<pix2.getWidth() && pix2.getPixel(x+1,y+1)==1) pix2.setPixel(x+1,y+1,2);
		                        
		                        if (y-1>0 && x-1>0 && pix2.getPixel(x-1,y-1)==1) pix2.setPixel(x-1,y-1,2);
		                        
		                        if (y-1>0 && x+1<pix2.getWidth() && pix2.getPixel(x+1,y-1)==1) pix2.setPixel(x+1,y-1,2);
		                        
		                        if (y+1<pix2.getHeight() && x-1>0 && pix2.getPixel(x-1,y+1)==1) pix2.setPixel(x-1,y+1,2);
		                    }
		            	}
		            }
		            
		            for(int y = 0; y < pix2.getHeight(); y++)
		            {
		            	for(int x = 0; x < pix2.getWidth(); x++)
		            	{
		            		if (pix2.getPixel(x, y) == 2)
		            		{
		            			pix2.setPixel(x,y-1,0);
		            		}
		            	}
		            }     		            
		            
		            imageSegmented.setImageBitmap(WriteFile.writeBitmap(pix2));
		            
/*		            baseapi.setImage(pixGray);
		            Pixa pixa = baseapi.getWords();
		            int boxes = pixa.size();
		            
		            for(int i = (int) Math.floor(pixa.size() *0.25); i < (int) Math.floor(pixa.size() *0.75); i++ )
		            {
		            	baseapi.setRectangle(pixa.getBoxRect(i));
		            	editText.setText(editText.getText() + baseapi.getUTF8Text());
		            }
*/	            
		           // editText.setText("1");
		            

		        }
		    }
		}
	
	Bitmap getScaledImage(int scale, Bitmap image)
	{
		return Bitmap.createScaledBitmap(image, image.getWidth()/scale, image.getHeight()/scale, true);
	}
	
	void handleClick(int id)
	{
		Pix tempPix;
		
		tempPix = AdaptiveMap.backgroundNormMorph(pix);
		tempPix = AdaptiveMap.backgroundNormMorph(pix, 16, 3, 128);
		tempPix = Binarize.otsuAdaptiveThreshold(pix);
		tempPix = Binarize.otsuAdaptiveThreshold(pix, pix.getWidth()+10, pix.getHeight()+10, 25, 25, 0);
		//tempPix = Binarize.sauvolaBinarizeTiled(pix, whsize, factor, nx, ny);
	}

}
