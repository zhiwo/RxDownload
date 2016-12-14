package zlc.season.rxdownloadproject.service_download;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.tbruyelle.rxpermissions2.RxPermissions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.functions.Consumer;
import zlc.season.rxdownload2.RxDownload;
import zlc.season.rxdownload2.entity.DownloadEvent;
import zlc.season.rxdownload2.entity.DownloadFlag;
import zlc.season.rxdownload2.entity.DownloadStatus;
import zlc.season.rxdownloadproject.DownloadController;
import zlc.season.rxdownloadproject.R;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static android.os.Environment.getExternalStoragePublicDirectory;

public class ServiceDownloadActivity extends AppCompatActivity {
    final String saveName = "梦幻西游.apk";
    final String defaultPath = getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getPath();
    final String url = "http://downali.game.uc.cn/wm/6/6/MY-1.98.0_uc_platform2_3306918_082452919a00.apk";

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.img)
    ImageView mImg;
    @BindView(R.id.percent)
    TextView mPercent;
    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.size)
    TextView mSize;
    @BindView(R.id.status)
    TextView mStatusText;
    @BindView(R.id.action)
    Button mAction;

    private RxDownload mRxDownload;
    private DownloadController mDownloadController;

    @OnClick(R.id.action)
    public void onClick() {
        mDownloadController.handleClick(new DownloadController.Callback() {
            @Override
            public void startDownload() {
                start();
            }

            @Override
            public void pauseDownload() {
                pause();
            }

            @Override
            public void cancelDownload() {
                cancel();
            }

            @Override
            public void install() {
                installApk();
            }
        });
    }

    @OnClick(R.id.finish)
    public void onClickFinish() {
        ServiceDownloadActivity.this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_download);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);

        String icon = "http://image.coolapk.com/apk_logo/2015/0330/12202_1427696232_8648.png";
        Picasso.with(this).load(icon).into(mImg);

        mRxDownload = RxDownload.getInstance().context(this);
        mDownloadController = new DownloadController(mStatusText, mAction);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRxDownload.receiveDownloadStatus(url)
                .subscribe(new Consumer<DownloadEvent>() {
                    @Override
                    public void accept(DownloadEvent downloadEvent) throws Exception {
                        if (downloadEvent.getFlag() == DownloadFlag.FAILED) {
                            Throwable throwable = downloadEvent.getError();
                            Log.w("Error", throwable);
                        }
                        mDownloadController.setEvent(downloadEvent);
                        updateProgress(downloadEvent);
                    }
                });
    }

    private void updateProgress(DownloadEvent event) {
        DownloadStatus status = event.getDownloadStatus();
        mProgress.setIndeterminate(status.isChunked);
        mProgress.setMax((int) status.getTotalSize());
        mProgress.setProgress((int) status.getDownloadSize());
        mPercent.setText(status.getPercent());
        mSize.setText(status.getFormatStatusString());
    }

    private void installApk() {
        Uri uri = Uri.fromFile(mRxDownload.getRealFiles(saveName, defaultPath)[0]);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        startActivity(intent);
    }

    private void start() {
        RxPermissions.getInstance(this)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .doOnNext(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean granted) throws Exception {
                        if (!granted) {
                            throw new RuntimeException("no permission");
                        }
                    }
                })
                .compose(mRxDownload.<Boolean>transformService(url, saveName, defaultPath))
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        Toast.makeText(ServiceDownloadActivity.this, "下载开始", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void pause() {
        mRxDownload.pauseServiceDownload(url).subscribe();
    }

    private void cancel() {
        mRxDownload.cancelServiceDownload(url).subscribe();
    }
}
