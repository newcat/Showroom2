package robertboschgmbh.test;

/*********************************************************************/
/**  Dateiname: DetailViewActivityEdit.java                         **/
/**                                                                 **/
/**  Beschreibung:  Bearbeitet den Inhalt eines Projektes           **/
/**                                                                 **/
/**  Autoren: Frederik Wagner, Lukas Schultt, Leunar Kalludra,      **/
/**           Jonathan Lessing, Marcel Vetter, Leopold Ormos        **/
/**           Merlin Baudert, Rino Grupp, Hannes Kececi             **/
/**                                                                 **/
/*********************************************************************/

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import dataloading.AsyncImageLoader;
import dataloading.ImageFileHelper;
import dataloading.XmlDataManager;
import filechooser.FileChooser;
import models.*;

public class DetailViewActivityEdit extends AppCompatActivity {

    //Indizes für ViewSets
    private static final int BLOCK_LAYOUT = 0;
    private static final int BLOCK_TITLE = 1;
    private static final int SB1_TEXT = 2;
    private static final int SB1_IMAGE_LAYOUT = 3;
    private static final int SB1_IMAGE = 4;
    private static final int SB1_SUBTITLE = 5;
    private static final int SB1_BTN_LOAD_IMAGE = 6;
    private static final int SB2_TEXT = 7;
    private static final int SB2_IMAGE_LAYOUT = 8;
    private static final int SB2_IMAGE = 9;
    private static final int SB2_SUBTITLE = 10;
    private static final int SB2_BTN_LOAD_IMAGE = 11;

    private TimerThread timerThread;
    private static boolean foreground = true;

    private ImageView buttonLeft, buttonRight;//Scrollbuttons

    private int leftBlockIndex = 0; //Index des linken Blocks
    private int blockCount = 0; //Anzahl der Blöcke im aktuellen Projekt

    private RadioButton rbSb1Text;
    private RadioButton rbSb2Text;
    private String currentTag = "";

    private boolean isNewProject = false;

    private ProjectModel model; //Aktuelles Projekt

    //Speichert Views
    SparseArray<View> block1ViewSet = new SparseArray<>();
    SparseArray<View> block2ViewSet = new SparseArray<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ContentView
        setContentView(R.layout.activity_detail_view_edit);

        //Toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        //Speicherbutton
        ImageButton imageButton1 = (ImageButton)findViewById(R.id.imageSave);
        imageButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //save new project data
                saveBlocks();
                XmlDataManager.changeProject(model);

                Intent i = new Intent(DetailViewActivityEdit.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
                }
            });

        //EndButton
        ImageButton imageButton2 = (ImageButton)findViewById(R.id.imageEnd);
        imageButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(DetailViewActivityEdit.this);
                builder.setTitle("Achtung");
                builder.setMessage("Möchten sie das Projekt schließen ohne zu speichern?");

                final View v = view;
                builder.setPositiveButton("JA", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(DetailViewActivityEdit.this, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(i);
                        finish();
                        dialog.dismiss();
                    }});

                builder.setNegativeButton("NEIN", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
            }});

                AlertDialog alert = builder.create();
                alert.show();
        }});

        //Get the corresponding model for this activity
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            model = null;
        } else {
            model = (ProjectModel)extras.get("model");
        }

        //Put the image views into variables so we save on findViewById-calls
        buttonLeft = (ImageView) findViewById(R.id.imageButtonLeft_edit);
        buttonRight = (ImageView) findViewById(R.id.imageButtonRight_edit);

        fillViewSets();
        setOnLongClickListeners();

        if (model == null) {
            //create a new project
            isNewProject = true;
            model = XmlDataManager.initializeProject(Environment.getExternalStorageDirectory());
            if (model == null) {
                //failed to create project folder
                Toast.makeText(this, "Konnte Projektordner nicht erstellen", Toast.LENGTH_LONG).show();
                Intent i = new Intent(DetailViewActivityEdit.this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
            } else {
                new ProjectSettingsDialog(this, model).show();
            }
        } else {
            //Get total block counts
            blockCount = model.getBlocks().size();

            //Set the project title
            TextView title = (TextView) findViewById(R.id.tvProjectTitle);
            title.setText(model.getTitle());
        }

        updateBlocks();
        checkButtonVisibility();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_3, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings4){

            //adding new Block
            addNewBlock();

        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewBlock() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        View v = inflater.inflate(R.layout.dialog_new_block, null);
        rbSb1Text = (RadioButton)v.findViewById(R.id.rbUpperText);
        rbSb2Text = (RadioButton)v.findViewById(R.id.rbLowerText);

        builder.setView(v);
        builder.setTitle(R.string.dialog_title);

        builder.setPositiveButton(R.string.dialog_btnAdd, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                DetailViewActivityEdit.this.onDialogResult(
                        rbSb1Text.isChecked() ? SubBlockType.TEXT : SubBlockType.IMAGE,
                        rbSb2Text.isChecked() ? SubBlockType.TEXT : SubBlockType.IMAGE
                );
            }
        });

        builder.setNegativeButton(R.string.dialog_btnCancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        builder.create().show();

    }

    public void onDialogResult(SubBlockType sb1Type, SubBlockType sb2Type) {

        saveBlocks();

        SubBlockModel sb1, sb2 = null;

        sb1 = createEmptySubblock(sb1Type);

        if (sb1Type != sb2Type) {
            sb2 = createEmptySubblock(sb2Type);
        }

        BlockModel b = new BlockModel("", sb1, sb2);
        model.addBlock(b);

        blockCount = model.getBlocks().size();

        //Move view so the new block is visible
        leftBlockIndex = blockCount - 2;
        if (leftBlockIndex < 0)
            leftBlockIndex = 0;

        updateBlocks();
        checkButtonVisibility();

    }

    private SubBlockModel createEmptySubblock(SubBlockType type) {
        if (type == SubBlockType.TEXT)
            return new SubBlockModel("");
        else
            return new SubBlockModel("", "");
    }

    //Füllt alle Views in die SparseArrays
    private void fillViewSets() {

        //Fill view set for block 1
        block1ViewSet.put(BLOCK_LAYOUT, findViewById(R.id.block1_layout_edit));
        block1ViewSet.put(BLOCK_TITLE, findViewById(R.id.block1_title_edit));
        block1ViewSet.put(SB1_TEXT, findViewById(R.id.block1_sb1_text_edit));
        block1ViewSet.put(SB1_IMAGE_LAYOUT, findViewById(R.id.block1_sb1_imageLayout_edit));
        block1ViewSet.put(SB1_IMAGE, findViewById(R.id.block1_sb1_image_edit));
        block1ViewSet.put(SB1_SUBTITLE, findViewById(R.id.block1_sb1_subtitle_edit));
        block1ViewSet.put(SB1_BTN_LOAD_IMAGE, findViewById(R.id.block1_sb1_btnLoadImage));
        block1ViewSet.put(SB2_TEXT, findViewById(R.id.block1_sb2_text_edit));
        block1ViewSet.put(SB2_IMAGE_LAYOUT, findViewById(R.id.block1_sb2_imageLayout_edit));
        block1ViewSet.put(SB2_IMAGE, findViewById(R.id.block1_sb2_image_edit));
        block1ViewSet.put(SB2_SUBTITLE, findViewById(R.id.block1_sb2_subtitle_edit));
        block1ViewSet.put(SB2_BTN_LOAD_IMAGE, findViewById(R.id.block1_sb2_btnLoadImage));

        //Fill view set for block 2
        block2ViewSet.put(BLOCK_LAYOUT, findViewById(R.id.block2_layout_edit));
        block2ViewSet.put(BLOCK_TITLE, findViewById(R.id.block2_title_edit));
        block2ViewSet.put(SB1_TEXT, findViewById(R.id.block2_sb1_text_edit));
        block2ViewSet.put(SB1_IMAGE_LAYOUT, findViewById(R.id.block2_sb1_imageLayout_edit));
        block2ViewSet.put(SB1_IMAGE, findViewById(R.id.block2_sb1_image_edit));
        block2ViewSet.put(SB1_SUBTITLE, findViewById(R.id.block2_sb1_subtitle_edit));
        block2ViewSet.put(SB1_BTN_LOAD_IMAGE, findViewById(R.id.block2_sb1_btnLoadImage));
        block2ViewSet.put(SB2_TEXT, findViewById(R.id.block2_sb2_text_edit));
        block2ViewSet.put(SB2_IMAGE_LAYOUT, findViewById(R.id.block2_sb2_imageLayout_edit));
        block2ViewSet.put(SB2_IMAGE, findViewById(R.id.block2_sb2_image_edit));
        block2ViewSet.put(SB2_SUBTITLE, findViewById(R.id.block2_sb2_subtitle_edit));
        block2ViewSet.put(SB2_BTN_LOAD_IMAGE, findViewById(R.id.block2_sb2_btnLoadImage));

    }

    private void setOnLongClickListeners() {
        //Setup the onLongClickListeners for the ImageViews
        View.OnLongClickListener listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                DetailViewActivityEdit.this.onBtnImageLoadClick(view);
                return true;
            }
        };

        block1ViewSet.get(SB1_IMAGE).setOnLongClickListener(listener);
        block1ViewSet.get(SB2_IMAGE).setOnLongClickListener(listener);
        block2ViewSet.get(SB1_IMAGE).setOnLongClickListener(listener);
        block2ViewSet.get(SB2_IMAGE).setOnLongClickListener(listener);

    }

    //Füllt Blöcke mit Daten
    private void updateBlocks() {

        //load left block
        if (blockCount == 0) {
            block1ViewSet.get(BLOCK_LAYOUT).setVisibility(View.GONE);
        } else {
            block1ViewSet.get(BLOCK_LAYOUT).setVisibility(View.VISIBLE);
            loadDataFromModel(model.getBlocks().get(leftBlockIndex), block1ViewSet);
        }

        if (blockCount <= 1) {
            //only display one block, stretched to the full display width,
            //so make the other block 'gone'
            block2ViewSet.get(BLOCK_LAYOUT).setVisibility(View.GONE);
        } else {
            block2ViewSet.get(BLOCK_LAYOUT).setVisibility(View.VISIBLE);
            loadDataFromModel(model.getBlocks().get(leftBlockIndex + 1), block2ViewSet);
        }

    }

    //Füllt Views von einem Block mit Daten
    private void loadDataFromModel(BlockModel bm, SparseArray<View> viewSet) {

        String blockTitle = bm.getTitle();
        SubBlockModel sb1 = bm.getSubBlock1();
        SubBlockModel sb2 = bm.getSubBlock2();

        EditText blockTitleView = (EditText)viewSet.get(BLOCK_TITLE);
        if (blockTitle != null && !blockTitle.equals("")) {
            blockTitleView.setText(blockTitle);
        } else {
            blockTitleView.setText("");
        }

        if (sb1 != null) {
            if (sb1.getType() == SubBlockType.TEXT) {

                viewSet.get(SB1_IMAGE_LAYOUT).setVisibility(View.GONE);

                EditText textView = (EditText)viewSet.get(SB1_TEXT);
                textView.setVisibility(View.VISIBLE);
                textView.setText(sb1.getText());

                //even though the image view will be invisible for now, reset the image
                //so the GC can collect it
                ((ImageView)viewSet.get(SB1_IMAGE)).setImageBitmap(null);

            } else {

                viewSet.get(SB1_TEXT).setVisibility(View.GONE);
                viewSet.get(SB1_IMAGE_LAYOUT).setVisibility(View.VISIBLE);

                String subtitle = sb1.getSubtitle();
                EditText subtitleView = (EditText) viewSet.get(SB1_SUBTITLE);
                if (subtitle != null && !subtitle.equals("")) {
                    subtitleView.setText(subtitle);
                } else {
                    subtitleView.setText("");
                }

                if (sb1.getImage() == null || sb1.getImage().equals("")) {

                    viewSet.get(SB1_BTN_LOAD_IMAGE).setVisibility(View.VISIBLE);

                    ImageView imageView = (ImageView)viewSet.get(SB1_IMAGE);
                    imageView.setVisibility(View.GONE);
                    imageView.setImageBitmap(null);

                } else {

                    viewSet.get(SB1_BTN_LOAD_IMAGE).setVisibility(View.GONE);

                    ImageView imageView = (ImageView)viewSet.get(SB1_IMAGE);
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(null);
                    AsyncImageLoader.setImageToImageView(sb1.getImage(), imageView,
                            imageView.getWidth(), imageView.getHeight());

                }

            }
        } else {

            viewSet.get(SB1_TEXT).setVisibility(View.GONE);
            viewSet.get(SB1_IMAGE_LAYOUT).setVisibility(View.GONE);

            //even though the image view will be invisible for now, reset the image
            //so the GC can collect it
            ((ImageView)viewSet.get(SB1_IMAGE)).setImageBitmap(null);
        }

        if (sb2 != null) {
            if (sb2.getType() == SubBlockType.TEXT) {

                viewSet.get(SB2_IMAGE_LAYOUT).setVisibility(View.GONE);

                EditText textView = (EditText)viewSet.get(SB2_TEXT);
                textView.setVisibility(View.VISIBLE);
                textView.setText(sb2.getText());

                //even though the image view will be invisible for now, reset the image
                //so the GC can collect it
                ((ImageView)viewSet.get(SB2_IMAGE)).setImageBitmap(null);

            } else {

                viewSet.get(SB2_TEXT).setVisibility(View.GONE);
                viewSet.get(SB2_IMAGE_LAYOUT).setVisibility(View.VISIBLE);

                String subtitle = sb2.getSubtitle();
                EditText subtitleView = (EditText) viewSet.get(SB2_SUBTITLE);
                if (subtitle != null && !subtitle.equals("")) {
                    subtitleView.setText(subtitle);
                } else {
                    subtitleView.setText("");
                }

                if (sb2.getImage() == null || sb2.getImage().equals("")) {

                    viewSet.get(SB2_BTN_LOAD_IMAGE).setVisibility(View.VISIBLE);

                    ImageView imageView = (ImageView)viewSet.get(SB2_IMAGE);
                    imageView.setVisibility(View.GONE);
                    imageView.setImageBitmap(null);

                } else {

                    viewSet.get(SB2_BTN_LOAD_IMAGE).setVisibility(View.GONE);

                    ImageView imageView = (ImageView)viewSet.get(SB2_IMAGE);
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(null);
                    AsyncImageLoader.setImageToImageView(sb2.getImage(), imageView,
                            imageView.getWidth(), imageView.getHeight());

                }

            }
        } else {

            viewSet.get(SB2_TEXT).setVisibility(View.GONE);
            viewSet.get(SB2_IMAGE_LAYOUT).setVisibility(View.GONE);

            //even though the image view will be invisible for now, reset the image
            //so the GC can collect it
            ((ImageView)viewSet.get(SB2_IMAGE)).setImageBitmap(null);
        }

    }


    //write textchanges in ProjectModel
    public void saveBlocks() {

        if (blockCount == 0 || leftBlockIndex < 0)
            return;

        EditText blockTitleView1 = (EditText)block1ViewSet.get(BLOCK_TITLE);
        model.getBlocks().get(leftBlockIndex).setTitle( blockTitleView1.getText().toString());

        if (model.getBlocks().get(leftBlockIndex).getSubBlock1() != null) {
            if (model.getBlocks().get(leftBlockIndex).getSubBlock1().getType() == SubBlockType.TEXT) {

                EditText textView1 = (EditText) block1ViewSet.get(SB1_TEXT);
                model.getBlocks().get(leftBlockIndex).getSubBlock1().setText(textView1.getText().toString());

            } else {

                EditText subtitleView1 = (EditText) block1ViewSet.get(SB1_SUBTITLE);
                model.getBlocks().get(leftBlockIndex).getSubBlock1().setSubtitle(subtitleView1.getText().toString());

            }
        }

        if (model.getBlocks().get(leftBlockIndex).getSubBlock2() != null) {
            if (model.getBlocks().get(leftBlockIndex).getSubBlock2().getType() == SubBlockType.TEXT) {

                EditText textView2 = (EditText) block1ViewSet.get(SB2_TEXT);
                model.getBlocks().get(leftBlockIndex).getSubBlock2().setText(textView2.getText().toString());

            } else {

                EditText subtitleView2 = (EditText) block1ViewSet.get(SB2_SUBTITLE);
                model.getBlocks().get(leftBlockIndex).getSubBlock2().setSubtitle(subtitleView2.getText().toString());

            }
        }

        //check if there are 2 blocks
        if(blockCount>1) {


            EditText blockTitleView2 = (EditText)block2ViewSet.get(BLOCK_TITLE);
            model.getBlocks().get(leftBlockIndex+1).setTitle( blockTitleView2.getText().toString());

            if (model.getBlocks().get(leftBlockIndex + 1).getSubBlock1() != null) {
                if (model.getBlocks().get(leftBlockIndex + 1).getSubBlock1().getType() == SubBlockType.TEXT) {

                    EditText textView3 = (EditText) block2ViewSet.get(SB1_TEXT);
                    model.getBlocks().get(leftBlockIndex + 1).getSubBlock1().setText(textView3.getText().toString());

                } else {

                    EditText subtitleView3 = (EditText) block2ViewSet.get(SB1_SUBTITLE);
                    model.getBlocks().get(leftBlockIndex + 1).getSubBlock1().setSubtitle(subtitleView3.getText().toString());

                }
            }

            if (model.getBlocks().get(leftBlockIndex + 1).getSubBlock2() != null) {
                if (model.getBlocks().get(leftBlockIndex + 1).getSubBlock2().getType() == SubBlockType.TEXT) {

                    EditText textView4 = (EditText) block2ViewSet.get(SB2_TEXT);
                    model.getBlocks().get(leftBlockIndex + 1).getSubBlock2().setText(textView4.getText().toString());

                } else {

                    EditText subtitleView4 = (EditText) block2ViewSet.get(SB2_SUBTITLE);
                    model.getBlocks().get(leftBlockIndex + 1).getSubBlock2().setSubtitle(subtitleView4.getText().toString());

                }
            }
        }

    }

    //Nach links scrollen
    public void swipeLeft(View view) {

        saveBlocks();

        if (leftBlockIndex > 0)
            leftBlockIndex--;

        updateBlocks();
        checkButtonVisibility();
    }

    //Nach rechts scrollen
    public void swipeRight(View view) {

        saveBlocks();

        if (leftBlockIndex < blockCount - 2)
            leftBlockIndex++;

        updateBlocks();
        checkButtonVisibility();
    }

    //check which of the swipe buttons should be visible
    private void checkButtonVisibility() {

        if (leftBlockIndex > 0) {
            buttonLeft.setVisibility(View.VISIBLE);
        } else {
            buttonLeft.setVisibility(View.INVISIBLE);
        }

        if (leftBlockIndex < blockCount - 2) {
            buttonRight.setVisibility(View.VISIBLE);
        } else {
            buttonRight.setVisibility(View.INVISIBLE);
        }

    }

    public void onBtnImageLoadClick(View v) {

        if (v.getTag() == null)
            return;

        currentTag = (String)v.getTag();

        new FileChooser(this).setFileListener(new FileChooser.FileSelectedListener() {
            @Override
            public void fileSelected(File file) {
                DetailViewActivityEdit.this.applyImageFile(file);
            }
        }).showDialog();

    }

    private void applyImageFile(File file) {

        saveBlocks();

        File f = ImageFileHelper.copyFileToDirectory(this, file, model.getDirectory());

        if (f != null) {

            switch (currentTag) {
                case "block1_sb1":
                    model.getBlocks().get(leftBlockIndex).getSubBlock1().setImage(f.getAbsolutePath());
                    break;
                case "block1_sb2":
                    model.getBlocks().get(leftBlockIndex).getSubBlock2().setImage(f.getAbsolutePath());
                    break;
                case "block2_sb1":
                    model.getBlocks().get(leftBlockIndex + 1).getSubBlock1().setImage(f.getAbsolutePath());
                    break;
                case "block2_sb2":
                    model.getBlocks().get(leftBlockIndex + 1).getSubBlock2().setImage(f.getAbsolutePath());
                    break;
            }

            updateBlocks();

        }

    }

    public void onInfoButtonClicked(View v) {

        new ProjectSettingsDialog(this, model).show();

    }

    public void onProjectSettingsDialogFinish(boolean dialogResult, ProjectModel updatedModel) {
        if (dialogResult) {
            model = updatedModel;
            isNewProject = false;

            //Set the project title
            TextView title = (TextView) findViewById(R.id.tvProjectTitle);
            title.setText(model.getTitle());
        } else if (isNewProject) {
            //user pressed cancel, but this is a new project, so delete the newly created
            //project folder and go back to the main activity
            if (model.getDirectory() != null)
                model.getDirectory().delete();

            Intent i = new Intent(DetailViewActivityEdit.this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        }
    }

    Handler hander = new Handler(){
        public void handleMessage(Message m){
            Intent intent = new Intent (DetailViewActivityEdit.this, screensaver.class);
            startActivity(intent);
            timerThread.interrupt();
        }
    };

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            timerThread.reset();
            return false;
        }
    };

    @Override
    public void onStop(){
        super.onStop();
        foreground = false;
    }

    @Override
    public void onStart(){
        super.onStart();
        foreground = true;
        timerThread = new TimerThread();
        timerThread.setDelay(Integer.parseInt(getResources().getString(R.string.screensaver_delay)) * 60000  );
        timerThread.setContext(this);
        timerThread.start();
    }

    public class TimerThread extends Thread{
        long delay = 0;
        long endTime;
        DetailViewActivityEdit context;
        public void run(){
            endTime = System.currentTimeMillis()+delay;
            boolean b = false;
            while(System.currentTimeMillis()<endTime&&!b){
                if(context.isDestroyed()||!foreground){
                    b = true;
                }
            }
            if (!b) {
                hander.sendMessage(new Message());
            }
        }
        public void reset(){
            endTime = System.currentTimeMillis()+delay;
        }
        public void setDelay(long delay){
            this.delay = delay;
        }
        public void setContext(DetailViewActivityEdit context){
            this.context = context;
        }
    }
}