(function() {
    // CSSによるテキスト選択禁止の解除
    var style = document.createElement('style');
    style.innerHTML = '* { user-select: text !important; -webkit-user-select: text !important; -moz-user-select: text !important; -ms-user-select: text !important; }';
    document.head.appendChild(style);

    // JSによるイベント制限（コピー、カット、ペースト、コンテキストメニュー、選択）の解除
    var events = ['copy', 'cut', 'paste', 'contextmenu', 'selectstart', 'mousedown', 'mouseup'];
    events.forEach(function(event) {
        document.addEventListener(event, function(e) {
            e.stopPropagation();
        }, true);
    });
})();