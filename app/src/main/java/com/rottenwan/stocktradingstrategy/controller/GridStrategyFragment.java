package com.rottenwan.stocktradingstrategy.controller;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.rottenwan.stocktradingstrategy.R;
import com.rottenwan.stocktradingstrategy.db.GridDBHelper;
import com.rottenwan.stocktradingstrategy.model.GridStrategyData;
import com.rottenwan.stocktradingstrategy.utils.ExcelUtils;
import com.rottenwan.stocktradingstrategy.utils.LogUtil;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by hewei on 2016-04-21  .*/
public class GridStrategyFragment extends Fragment {
    private RadioGroup mRadioGroupGrid;
    private EditText mInvestmentAmount;
    private EditText mInitialPrice;
    private Button mExportGridTable;
    private Button mImportGridTable;
    private ListView mGridTableList;
    private GridStrategyData mGridStrategyData;

    private File mFile;
    private String[] mTitle = {"买入B档", "买入A档", "底仓", "卖出A档", "卖出B档"};
    private Double[] mSaveData;
    private GridDBHelper mDbHelper;
    private ArrayList<ArrayList<Double>> mGridList;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDbHelper = new GridDBHelper(getActivity());
        mDbHelper.open();
        mGridList = new ArrayList<>();
        mGridStrategyData = GridStrategyData.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_grid_strategy, container, false);
        mRadioGroupGrid = (RadioGroup) v.findViewById(R.id.radio_group_grid);
        mRadioGroupGrid.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.little_grid:
                        mGridStrategyData.setBuyGrid(0.03);
                        mGridStrategyData.setSellGrid(0.05);
                        break;
                    case R.id.small_grid:
                        mGridStrategyData.setBuyGrid(0.05);
                        mGridStrategyData.setSellGrid(0.1);
                        break;
                    case R.id.middle_grid:
                        mGridStrategyData.setBuyGrid(0.08);
                        mGridStrategyData.setSellGrid(0.15);
                        break;
                    case R.id.big_grid:
                        mGridStrategyData.setBuyGrid(0.12);
                        mGridStrategyData.setSellGrid(0.2);
                        break;
                    default:
                        break;
                }
            }
        });

        mInvestmentAmount = (EditText) v.findViewById(R.id.investment_amount);
        mInvestmentAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mGridStrategyData.setInvestmentAmount(Double.parseDouble(s.toString()));
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mInitialPrice = (EditText) v.findViewById(R.id.initial_price);
        mInitialPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mGridStrategyData.setInitialPrice(Double.parseDouble(s.toString()));
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        mExportGridTable = (Button) v.findViewById(R.id.export_grid_table);
        mExportGridTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSaveData = new Double[] {
                        mGridStrategyData.getBuyGrid(),
                        mGridStrategyData.getSellGrid(),
                        mGridStrategyData.getInvestmentAmount(),
                        mGridStrategyData.getInitialPrice() };

                if (canSave(mSaveData)) {
                    ContentValues values = new ContentValues();
                    values.put("buy_B", mGridStrategyData.computeBuyB());
                    values.put("buy_A", mGridStrategyData.computeBuyA());
                    values.put("init", mGridStrategyData.getInitialPrice());
                    values.put("sell_A", mGridStrategyData.computeSellA());
                    values.put("sell_B", mGridStrategyData.computeSellB());

                    long insert = mDbHelper.insert(GridDBHelper.DB_NAME, values);
                    if (insert > 0) {
                        initData();
                    }
                } else {
                    Toast.makeText(getActivity(), "内容填写不完整，请确认", Toast.LENGTH_SHORT).show();
                }
                /*
                double buyGrid = mGridStrategyData.getBuyGrid();
                double sellGrid = mGridStrategyData.getSellGrid();
                double investmentAmount = mGridStrategyData.getInvestmentAmount() * 10000;
                double initialPrice = mGridStrategyData.getInitialPrice();

                LogUtil.d("buyGrid = " + buyGrid + ", sellGrid = " + sellGrid
                    + ", investmentAmount = " + investmentAmount + ", initialPrice = " + initialPrice);
                    */
            }
        });

        mImportGridTable = (Button) v.findViewById(R.id.import_grid_table);
        mImportGridTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<GridStrategyData> gridList = (ArrayList<GridStrategyData>) ExcelUtils
                        .read2DB(new File(getSDPath() + "/GridStrategy/grid.xls"), getActivity());
                mGridTableList.setAdapter(new GridAdapter(getActivity(), gridList));
            }
        });

        mGridTableList = (ListView) v.findViewById(R.id.grid_table_list);

        View contentHeader = LayoutInflater.from(getActivity()).inflate(
                R.layout.listview_header, null);
        mGridTableList.addHeaderView(contentHeader);

        return v;
    }

    public void initData() {
        mFile = new File(getSDPath() + "/GridStrategy");
        boolean res = makeDir(mFile);
        if (!res) {
            LogUtil.d("make dir fail or directory already existed.");
        }
        ExcelUtils.initExcel(mFile.toString() + "grid.xls", mTitle);
        ExcelUtils.writeObjListToExcel(getGridData(),
                getSDPath() + "/GridStrategy/grid.xls", getActivity());
    }

    private ArrayList<ArrayList<Double>> getGridData() {
        Cursor mCrusor = mDbHelper.exeSql("select * from grid_strategy");
        while (mCrusor.moveToNext()) {
            ArrayList<Double> beanList = new ArrayList<>();
            beanList.add(mCrusor.getDouble(1));
            beanList.add(mCrusor.getDouble(2));
            beanList.add(mCrusor.getDouble(3));
            beanList.add(mCrusor.getDouble(4));
            beanList.add(mCrusor.getDouble(5));

            mGridList.add(beanList);
        }
        mCrusor.close();
        return mGridList;
    }


    public static boolean makeDir(File dir) {
        if (!dir.getParentFile().exists()) {
            makeDir(dir.getParentFile());
        }
        return dir.mkdir();
    }

    public String getSDPath() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        }
        String dir = null;
        if (sdDir != null) {
            dir = sdDir.toString();
        }
        return dir;

    }

    private boolean canSave(Double[] data) {
        boolean isOk = true;
        for (int i = 0; i < data.length; i++) {
            if (i > 0 && i < data.length) {
                if (data[i] <= 0) {
                    isOk = false;
                }
            }
        }
        return isOk;
    }
}
