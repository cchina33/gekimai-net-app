(function() {
    const url = window.location.href;
    let results = [];

    // ==========================================
    // ① オンゲキのプレイ履歴集計処理
    // ==========================================
    if (url.startsWith("https://ongeki-net.com/ongeki-mobile/record/playlog/")) {
        const dateSpans = document.querySelectorAll('span.f_r.f_12.h_10');
        const playCountByDay = {};

        dateSpans.forEach(span => {
            const date = span.textContent.split(' ')[0];
            playCountByDay[date] = (playCountByDay[date] || 0) + 1;
        });

        for (const date in playCountByDay) {
            const count = playCountByDay[date];
            const totalGP = count * 40;
            const cost = Math.ceil(totalGP / 120) * 100;
            results.push({ date, count, cost });
        }
    }
    // ==========================================
    // ② maimaiのプレイ履歴集計処理
    // ==========================================
    else if (url.startsWith("https://maimaidx.jp/maimai-mobile/record/")) {
        let playCountPerDay = {};
        let lastDay = null;
        let lastTrack = null;

        document.querySelectorAll(".playlog_top_container").forEach(container => {
            let spans = container.querySelectorAll(".sub_title span");
            if (spans.length >= 2) {
                let track = spans[0].textContent.trim();
                let day = spans[1].textContent.trim().split(" ")[0];
                if (track === "TRACK 01") {
                    playCountPerDay[day] = (playCountPerDay[day] || 0) + 1;
                }
                lastDay = day;
                lastTrack = track;
            }
        });

        if (lastDay && lastTrack !== "TRACK 01") {
            playCountPerDay[lastDay] = (playCountPerDay[lastDay] || 0) + 1;
        }

        Object.entries(playCountPerDay).forEach(([day, count]) => {
            results.push({ date: day, count, cost: count * 100 });
        });
    }

    // ==========================================
    // 💡 Androidの窓口（Bridge）へデータを送信
    // ==========================================
    if (window.AndroidBridge && typeof window.AndroidBridge.sendResults === "function") {
        window.AndroidBridge.sendResults(JSON.stringify(results));
    }
})();