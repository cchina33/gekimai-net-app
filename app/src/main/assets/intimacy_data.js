(function() {
    // エラーログをアプリ側に通知するための関数
    function sendError(msg) {
        console.error(msg);
        if (window.AndroidBridge && window.AndroidBridge.showToast) {
            window.AndroidBridge.showToast("解析エラー: " + msg);
        }
    }

    async function get(u) {
        return new Promise(r => {
            var x = new XMLHttpRequest();
            x.open('GET', u);
            x.responseType = 'document';
            x.onload = () => r(x.response);
            x.onerror = () => { sendError("通信失敗: " + u); r(null); };
            x.send();
        });
    }

    function parse(d) {
        if (!d) return [];
        // characterDetail へのアクションを持つフォームを抽出
        var forms = Array.from(d.forms).filter(f => f.action && f.action.indexOf('characterDetail') >= 0);
        return forms.map(f => {
            var c = f.querySelector('.character_friendly_container') || f.querySelector('.character_friendly_deluxe_container');
            var lv = 0;
            if (c) {
                var hundred = 0, ten = 0, one = 0;
                var imgs = Array.from(c.querySelectorAll('img'));
                imgs.forEach(img => {
                    var src = img.src || "";
                    if (src.indexOf('rebirth_num_') !== -1) {
                        var m = src.match(/rebirth_num_(\d+)\./);
                        if (m) hundred = parseInt(m[1], 10) * 100;
                    } else if (src.indexOf('num_') !== -1) {
                        var m = src.match(/num_(\d+)\./);
                        if (m) {
                            var n = parseInt(m[1], 10);
                            if (n >= 10) ten = n;
                            else one = n;
                        }
                    }
                });
                lv = hundred + ten + one;
            }

            // idxの安全な取得
            var idxVal = "";
            if (f.elements && f.elements.idx) {
                idxVal = f.elements.idx.value;
            } else {
                var idxInput = f.querySelector('input[name="idx"]');
                if (idxInput) idxVal = idxInput.value;
            }

            return { idx: idxVal, friendly: lv, next: 0 };
        });
    }

    Promise.all([
        get('/ongeki-mobile/character/'),
        get('/ongeki-mobile/collection/intimateUpItem/'),
        get('/ongeki-mobile/home/playerDataDetail/')
    ]).then(docs => {
        try {
            if (!docs[0] || !docs[1] || !docs[2]) {
                sendError("一部のページデータの取得に失敗しました。再ログインしてください。");
                return;
            }

            var moneyEl = docs[2].querySelector('table.t_l.f_13 tr td+td+td');

            // 親密度アイテム（コレクションページ）からの抽出
            var presentItems = Array.from(docs[1].querySelectorAll("span.d_ib.m_3.p_5")).map(s => s.innerText.split("個")[0]);

            var res = {
                friendly_data: parse(docs[0]),
                item_big: parseInt(presentItems[2], 10) || 0,
                item_mid: parseInt(presentItems[1], 10) || 0,
                item_small: parseInt(presentItems[0], 10) || 0,
                money_data: moneyEl ? moneyEl.innerText.replace(/,/g, '').split('（')[0] : "未取得"
            };

            if (window.AndroidBridge && window.AndroidBridge.receiveOverPrintData) {
                window.AndroidBridge.receiveOverPrintData(JSON.stringify(res));
            } else {
                sendError("AndroidBridge が見つかりません。");
            }
        } catch (e) {
            sendError(e.message);
        }
    }).catch(err => {
        sendError("Promise全体のエラー: " + err);
    });
})();