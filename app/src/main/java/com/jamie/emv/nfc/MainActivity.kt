package com.jamie.emv.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.devnied.emvnfccard.parser.EmvTemplate
import com.jamie.emv.nfc.emvnfc.PcscProvider
import com.jamie.emv.nfc.ui.theme.EMVNFCTheme
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var mNfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this)

        enableEdgeToEdge()
        setContent {
            EMVNFCTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (mNfcAdapter != null) {
            val options = Bundle()
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)

            mNfcAdapter!!.enableReaderMode(
                this,
                this,
                NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        NfcAdapter.FLAG_READER_NFC_BARCODE or
                        NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                options
            )
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        val isoDep: IsoDep?
        try {
            isoDep = IsoDep.get(tag)
            if (isoDep != null) {
                (getSystemService(VIBRATOR_SERVICE) as Vibrator).vibrate(
                    VibrationEffect.createOneShot(
                        150,
                        10
                    )
                )
            }
            isoDep.connect()
            val provider = PcscProvider()
            provider.setmTagCom(isoDep)
            val config = EmvTemplate.Config()
                .setContactLess(true)
                .setReadAllAids(true)
                .setReadTransactions(true)
                .setRemoveDefaultParsers(false)
                .setReadAt(true)
            val parser = EmvTemplate.Builder()
                .setProvider(provider)
                .setConfig(config)
                .build()
            val card = parser.readEmvCard()
            val cardNumber = card.cardNumber
            val cardHolder = card.holderFirstname + " " + card.holderLastname
            val cardType = card.type.getName()
            Log.d(
                "PaymentResultCardNumber: ",
                "Card Number: $cardNumber\nCard Holder: $cardHolder\nType: $cardType"
            )

            val expireDate = card.expireDate
            var date = LocalDate.of(1999, 12, 31)
            if (expireDate != null) {
                date = expireDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            Log.d("PaymentResultDate: ", date.toString())
            try {
                isoDep.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    Text(
        text = "Tap your card!",
        modifier = modifier
    )
}