package la.yakumo.ganash;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OnAccountsUpdateListener;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.io.IOException;
import java.util.ArrayList;

public class GanashActivity extends Activity
{
    private static final String TAG = Constructs.LOG_TAG;
    private static final int RESULT_CREDENTIAL_CHECK = 1;

    private ListView listView;
    private AccountListAdapter accountList;
    private Handler handler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        listView = new ListView(this);
        accountList = new AccountListAdapter(this);
        listView.setAdapter(accountList);
        setContentView(listView);

        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(
                AdapterView<?> parent,
                View view,
                int position,
                long id)
            {
                AccountListAdapter.AccountListData ld =
                    (AccountListAdapter.AccountListData) view.getTag();
                Log.i(TAG, "ld:"+ld);
                if (ld instanceof AccountListAdapter.AccountListData) {
                    if (ld.getViewType() == AccountListAdapter.AccountListData.VIEWTYPE_ADDACCOUNT) {
                        startActivity(new Intent(Settings.ACTION_ADD_ACCOUNT));
                    }
                    else {
                        AccountManager am =
                            AccountManager.get(GanashActivity.this);
                        am.getAuthToken(
                            ld.getAccount(), "ah", true,
                            new SelectAccountCallback(),
                            handler);
                    }
                }
            }
        });

        AccountManager am = AccountManager.get(this);
        am.addOnAccountsUpdatedListener(new OnAccountsUpdateListener() {
            public void onAccountsUpdated(Account[] accounts)
            {
                accountList.setAccounts(accounts);
                listView.requestLayout();
            }
        }, handler, true);
    }

    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data)
    {
        switch (requestCode) {
        case RESULT_CREDENTIAL_CHECK:
            Log.i(TAG, "result:"+data);
            break;
        default:
            break;
        }

    }

    public class SelectAccountCallback
        implements AccountManagerCallback<Bundle>
    {
        public void run(AccountManagerFuture<Bundle> future)
        {
            Log.i(TAG, "select:"+future);
            try {
                Bundle bundle = future.getResult();
                Log.i(TAG, "flags:"+future.isDone()+","+future.isCancelled());
                Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                Log.i(TAG, "intent:"+intent);
                if (intent != null) {
                    startActivity(intent);
                }
                else {
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    Log.i(TAG, "token:"+token);
                }
            } catch (OperationCanceledException e) {
                Log.e(TAG, "operation canceled", e);
            } catch (IOException e) {
                Log.e(TAG, "I/O error", e);
            } catch (AuthenticatorException e) {
                Log.e(TAG, "authentication error", e);
            }
        }
    }

    public class AccountListAdapter
        extends BaseAdapter
    {
        public class AccountListData
        {
            public static final int VIEWTYPE_ACCOUNT = 1;
            public static final int VIEWTYPE_ADDACCOUNT = 2;

            private int viewType;
            private Account account;

            public AccountListData()
            {
                viewType = VIEWTYPE_ADDACCOUNT;
            }

            public AccountListData(Account account)
            {
                this.account = account;
                viewType = VIEWTYPE_ACCOUNT;
            }

            int getViewType()
            {
                return this.viewType;
            }

            Account getAccount()
            {
                return account;
            }
        }

        private Context context;
        private ArrayList<AccountListData> listData =
            new ArrayList<AccountListData>();

        public AccountListAdapter(Context context)
        {
            this.context = context;
            reloadAccounts();
        }

        public void setAccounts(Account[] accounts)
        {
            listData.clear();
            for (Account account : accounts) {
                if ("com.google".equals(account.type)) {
                    listData.add(new AccountListData(account));
                }
                Log.i(TAG, "name:"+account.name+"("+account.type+")");
            }
            listData.add(new AccountListData());
        }

        public void reloadAccounts()
        {
            AccountManager am = AccountManager.get(context);
            setAccounts(am.getAccounts());
        }

        public int getCount()
        {
            return listData.size();
        }

        public Object getItem(int position)
        {
            return listData.get(position);
        }

        public long getItemId(int position)
        {
            return position;
        }

        public int getItemViewType(int position)
        {
            AccountListData ld = (AccountListData) getItem(position);
            return ld.getViewType();
        }

        public View getView(
            int position,
            View convertView,
            ViewGroup parent)
        {
            AccountListData ld = (AccountListData) getItem(position);
            if (convertView == null) {
                switch (ld.getViewType()) {
                case AccountListData.VIEWTYPE_ACCOUNT:
                    convertView = View.inflate(
                        context,
                        R.layout.account_list_item,
                        null);
                    break;
                case AccountListData.VIEWTYPE_ADDACCOUNT:
                    convertView = new TextView(context);
                    break;
                default:
                    break;
                }
            }
            TextView label = null;
            ImageView icon = null;
            switch (ld.getViewType()) {
            case AccountListData.VIEWTYPE_ACCOUNT:
                label = (TextView) convertView.findViewById(R.id.account_text);
                icon = (ImageView) convertView.findViewById(R.id.account_type_icon);
                label.setText(ld.getAccount().name);
                break;
            case AccountListData.VIEWTYPE_ADDACCOUNT:
                label = (TextView) convertView;
                label.setText(R.string.add_account_label);
                break;
            default:
                break;
            }
            convertView.setTag(ld);
            return convertView;
        }

        public int getViewTypeCount()
        {
            return 2;
        }
    }
}
