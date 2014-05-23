package ace.io.ocr.receiptocr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.googlecode.leptonica.android.AdaptiveMap;
import com.googlecode.leptonica.android.Binarize;
import com.googlecode.leptonica.android.Convert;
import com.googlecode.leptonica.android.Enhance;
import com.googlecode.leptonica.android.Pix;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.leptonica.android.Rotate;
import com.googlecode.leptonica.android.Skew;
import com.googlecode.leptonica.android.WriteFile;
import com.googlecode.tesseract.android.TessBaseAPI;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class AutoTask extends AsyncTask<String, Integer, Boolean> {
	private static final String DESTINATION_DIR = Environment.getExternalStorageDirectory()+"/Android/data/ace.io.ocr.receiptocr/Files/";

	@Override
	protected Boolean doInBackground(String... urls) 
	{
		TessBaseAPI baseAPI = new TessBaseAPI();
		baseAPI.init("/storage/sdcard0/tesseract/", "eng", TessBaseAPI.OEM_TESSERACT_CUBE_COMBINED);
		
		//for (int i = 4; i < urls.length; i++)
		//for (int i = 3; i < 4; i++)
		//for (int i = 2; i < 4; i++)
		//for (int i = 0; i <2; i++)
		//int i = 3;
		for (int i = 2; i < urls.length; i++)
		{
			int rotate = 0;
            try{
            	ExifInterface exif = new ExifInterface(urls[i]);
            	int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            	switch(exifOrientation)
            	{
            	case ExifInterface.ORIENTATION_ROTATE_90:
            		rotate = 90;
            		break;
            	case ExifInterface.ORIENTATION_ROTATE_180:
            		rotate = 180;
            		break;
            	case ExifInterface.ORIENTATION_ROTATE_270:
            		rotate = 270;
            		break;
            	}
            }
            catch(IOException e){return false;}
            long start = System.currentTimeMillis();
            Bitmap bmp = getScaledImage(2, BitmapFactory.decodeFile(urls[i]));

            // write original image
        	try {
        		File path = new File(DESTINATION_DIR + "/image" + i + "/");
        		if(!path.exists())
                {
                	if(!path.mkdirs())
                		return false;
                }
        		FileOutputStream out = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/0_orig.JPG");
				bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
				out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
            Pix pix = ReadFile.readBitmap(bmp);
            bmp.recycle();
            
            if(rotate!=0)
            {
            	pix = Rotate.rotate(pix, rotate, true, false);
            	// write rotated image
            	try {
            		FileOutputStream out = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/1_rotated.JPG");
    				WriteFile.writeBitmap(pix).compress(Bitmap.CompressFormat.JPEG, 85, out);
    				out.close();
    			} catch (FileNotFoundException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
            }
            
            // Convert to grayscale
            pix = Convert.convertTo8(pix);
            //write grayscale image
            try {
        		FileOutputStream out = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/2_grayscale.JPG");
				WriteFile.writeBitmap(pix).compress(Bitmap.CompressFormat.JPEG, 85, out);
				out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            // Fix skew
            float skew = Skew.findSkew(pix);
			if(skew!=0)
			{
				pix = Rotate.rotate(pix, skew);
				
				// Write skew image
				try {
	        		FileOutputStream out = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/3_skew_" + skew + ".JPG");
					WriteFile.writeBitmap(pix.copy()).compress(Bitmap.CompressFormat.JPEG, 85, out);
					out.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			pix = AdaptiveMap.backgroundNormMorph(pix.copy());
			try {
        		FileOutputStream out = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/4_bgmorph.JPG");
				WriteFile.writeBitmap(pix.copy()).compress(Bitmap.CompressFormat.JPEG, 85, out);
				out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            pix = Binarize.otsuAdaptiveThreshold(pix, 48, 48, 9, 9, 0.1F);
            //write otsu image
            try {
        		FileOutputStream out = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/4_otsu.JPG");
				WriteFile.writeBitmap(pix.copy()).compress(Bitmap.CompressFormat.JPEG, 85, out);
				out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            baseAPI.setImage(pix.copy());
            String textResult = baseAPI.getUTF8Text();
            long timeRequired = System.currentTimeMillis() - start;
            
            //Pixa pixa = baseAPI.getWords();
			bmp = WriteFile.writeBitmap(pix.copy());
			Canvas canvas = new Canvas(bmp);
			FileOutputStream out = null;
			try{
				out = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/decode.txt");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			for(int j = 0; j<pixa.size(); j++)
//			{
//				Rect rect = pixa.getBoxRect(j);
				Paint paint =  new Paint();
				paint.setAlpha(0xFF);
				paint.setStyle(Style.STROKE);
			    paint.setStrokeWidth(2);
				
//				canvas.drawLine(rect.left, rect.bottom, rect.left, rect.top, paint);
//				canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paint);
//				canvas.drawLine(rect.right, rect.top, rect.left, rect.top, paint);
//				canvas.drawLine(rect.right, rect.top, rect.right, rect.bottom, paint);
//				// TODO: For each pixa write subimage plus char recognition image
//				Pix wordPix = pixa.getPix(j);
//				//wordPix = Enhance.unsharpMasking(wordPix, 3, .35f);
//				
//				// write sub image
//				try {
//            		FileOutputStream subout = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/5_word_" + j + ".JPG");
//    				WriteFile.writeBitmap(wordPix.copy()).compress(Bitmap.CompressFormat.JPEG, 85, subout);
//    				subout.close();
//    			} catch (FileNotFoundException e) {
//    				// TODO Auto-generated catch block
//    				e.printStackTrace();
//    			} catch (IOException e) {
//    				// TODO Auto-generated catch block
//    				e.printStackTrace();
//    			}
////				int depth = wordPix.getDepth();
////				if(depth==8)
////				{
////					wordPix = Binarize.otsuAdaptiveThreshold(wordPix);
////				
////					// write sub image otsu
////					try {
////	            		FileOutputStream subout = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/5_word_otsu_" + j + ".JPG");
////	    				WriteFile.writeBitmap(wordPix).compress(Bitmap.CompressFormat.JPEG, 85, subout);
////	    				subout.close();
////	    			} catch (FileNotFoundException e) {
////	    				// TODO Auto-generated catch block
////	    				e.printStackTrace();
////	    			} catch (IOException e) {
////	    				// TODO Auto-generated catch block
////	    				e.printStackTrace();
////	    			}
////				}
//				
//				// write translation
//				try {
//					baseAPI.setImage(wordPix.copy());
//					byte[] str = (baseAPI.getUTF8Text()+"\n").getBytes();
//					if(out!=null)out.write(str, 0, str.length);
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			pixa.recycle();
			
			// write word detection image
//			try {
//				if(out != null)out.close();
//        		FileOutputStream subout = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/6_word_boxes.JPG");
//				bmp.compress(Bitmap.CompressFormat.JPEG, 85, subout);
//				subout.close();
//				
//				subout = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/6_word_no_boxes.JPG");
//				(WriteFile.writeBitmap(pix.copy())).compress(Bitmap.CompressFormat.JPEG, 85, subout);
//				subout.close();
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			bmp.recycle();
//			int[] confiences = baseAPI.wordConfidences();
			int meanConfidence = baseAPI.meanConfidence();
//			ArrayList<Rect> regionRects = baseAPI.getRegions().getBoxRects();
//			ArrayList<Rect> lineRects = baseAPI.getTextlines().getBoxRects();
			ArrayList<Rect> wordRects = baseAPI.getWords().getBoxRects();
//			ArrayList<Rect> stripRects = baseAPI.getStrips().getBoxRects();
			
			// Draw boxes around words
			for (int j = 0; j < wordRects.size(); j++) {
				paint.setColor(Color.RED);
				paint.setStyle(Style.STROKE);
				paint.setStrokeWidth(2);
				Rect r = wordRects.get(j);
				canvas.drawRect(r, paint);
			} 
			
			try {
        		FileOutputStream subout = new FileOutputStream(DESTINATION_DIR + "/image" + i + "/6_word_boxes.JPG");
				bmp.compress(Bitmap.CompressFormat.JPEG, 85, subout);
				subout.close();
				byte[] str = ("Mean Confidence: " + meanConfidence + "\n\n" + textResult + "").getBytes();
				out.write(str);
				if(out != null)out.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Log.i("ACE", "Mean Confidence:" + meanConfidence);
		}
		baseAPI.end();
		Log.i("OCRReceipt", "Done");
		return true;
	}
	
	Bitmap getScaledImage(int scale, Bitmap image)
	{
		float scalew = 1200.0f/image.getWidth();
		float scaleh = 1600.0f/image.getHeight();
		float factor = Math.min(scalew, scaleh);
		factor = Math.min(factor, 1f);
		return Bitmap.createScaledBitmap(image, (int)Math.floor(image.getWidth()*factor), (int)Math.floor(image.getHeight()*factor), true);
//		return Bitmap.createScaledBitmap(image, image.getWidth()/scale, image.getHeight()/scale, true);
	}

}
