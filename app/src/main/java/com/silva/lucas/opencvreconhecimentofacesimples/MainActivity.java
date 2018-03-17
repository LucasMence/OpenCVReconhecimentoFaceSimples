package com.silva.lucas.opencvreconhecimentofacesimples;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    Button btnEscolherImagem, btnReconhecerImagem;

    ImageView imgImagem;

    private static int REQUISICAO_GALERIA = 1;


    //CARREGAMENTO INICIAL DA BIBLIOTECA DO OPENCV

    static {
        if (!OpenCVLoader.initDebug()) {
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "Biblioteca carregada com êxito");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Biblioteca do OpenCV nao encontrada, procurando biblioteca externa.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "Biblioteca do OpenCV encontrada com exito!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }


    //ACTIVITY DO PROJETO

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnEscolherImagem = (Button) findViewById(R.id.btnEscolherImagem);
        btnReconhecerImagem = (Button) findViewById(R.id.btnReconhecerImagem);
        imgImagem = (ImageView) findViewById(R.id.imgImagem);


        btnEscolherImagem.setOnClickListener(new View.OnClickListener(){
            @Override

            public void onClick(View v) {
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(i, REQUISICAO_GALERIA);
            }
        });

        btnReconhecerImagem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //BITMAP
                BitmapDrawable bitmapDrawable = ((BitmapDrawable) imgImagem.getDrawable());
                Bitmap bitmap;
                if(bitmapDrawable==null){
                    imgImagem.buildDrawingCache();
                    bitmap = imgImagem.getDrawingCache();
                    imgImagem.buildDrawingCache(false);
                }else
                {
                    bitmap = bitmapDrawable .getBitmap();
                }

                //INICIO DO PROCESSAMENTO PELO OPENCV
                Mat modeloMatematico = new Mat (bitmap.getWidth(), bitmap.getHeight(), CvType.CV_USRTYPE1);
                Utils.bitmapToMat(bitmap, modeloMatematico);

                Mat modeloMatematicoEscalaCinza = new Mat();
                Imgproc.cvtColor(modeloMatematico, modeloMatematicoEscalaCinza, Imgproc.COLOR_RGB2GRAY);

                Imgproc.equalizeHist(modeloMatematicoEscalaCinza, modeloMatematicoEscalaCinza);

                MatOfRect vetorDeDetectoes = new MatOfRect();

                CascadeClassifier detectorDeFaces;

                try {
                    InputStream xmlTreinamento = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                    File diretorioXml = getDir("xml", Context.MODE_PRIVATE);
                    File arquivoXml = new File(diretorioXml, "haarcascade_frontalface_default.xml");
                    FileOutputStream os = new FileOutputStream(arquivoXml);


                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = xmlTreinamento.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    xmlTreinamento.close();
                    os.close();

                    detectorDeFaces = new CascadeClassifier(arquivoXml.getAbsolutePath());

                    //ONDE RECONHECE AS FACES E JOGA NO VETOR DE LINHAS
                    detectorDeFaces.detectMultiScale(modeloMatematicoEscalaCinza,
                            vetorDeDetectoes,1.1,2,2,new Size(0,0),
                            new Size(modeloMatematicoEscalaCinza.width(),modeloMatematicoEscalaCinza.height()));

                    //DESENHA OS RETANGULOS COM OS VETORES
                    for (Rect retangulo : vetorDeDetectoes.toArray()) {
                        Imgproc.rectangle(modeloMatematico,
                                new Point(retangulo.x, retangulo.y),
                                new Point(retangulo.x + retangulo.width, retangulo.y + retangulo.height),
                                new Scalar(0, 255, 0));
                    }


                    Utils.matToBitmap(modeloMatematico, bitmap);

                } catch (Exception e) {
                    Log.e("OpenCVActivity", "Erro ao carregar biblioteca", e);
                }

                imgImagem.setImageBitmap(bitmap);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUISICAO_GALERIA && resultCode == RESULT_OK && null != data) {
            Uri imagemSelecionada = data.getData();

            Bitmap bitmap = null;
            try {
                bitmap = getUriBitmap(imagemSelecionada);
            } catch (IOException e) {
                e.printStackTrace();
            }
            imgImagem.setImageBitmap(bitmap);

        }

    }

    private Bitmap getUriBitmap(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }
}
