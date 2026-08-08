/* ========== 闲淘 双模式切换工具 ==========
 * 必须放在 <head> 中、页面 css 之后同步引入：
 *   <script src="./js/mode.js"></script>
 *
 * 功能：
 *   1. 模式判定：localStorage(xt_ui_mode) 记忆优先，无记忆时按屏幕宽度（>=992px 视为桌面）
 *   2. 在 <html> 上添加 xt-desktop / xt-mobile 与 page-xxx 类，供 css/desktop.css 使用
 *   3. 桌面模式注入顶部导航栏
 *   4. 右下角悬浮按钮一键切换模式（选择会记住）
 */
(function () {
  var KEY = 'xt_ui_mode';
  var saved = null;
  try { saved = localStorage.getItem(KEY); } catch (e) {}
  var mode = (saved === 'desktop' || saved === 'mobile')
    ? saved
    : (window.innerWidth >= 992 ? 'desktop' : 'mobile');

  var root = document.documentElement;
  root.classList.add('xt-' + mode);

  // 页面标识：index / goods-list / goods-detail / note-detail / note-edit / info / info-edit / other-info / login / login2 / message / chat
  var page = (location.pathname.split('/').pop() || 'index.html').replace(/\.html$/i, '') || 'index';
  root.classList.add('page-' + page);

  // 供页面 JS 使用的全局 API（桌面模式滚动兼容等）
  window.XtMode = {
    isDesktop: function () { return root.classList.contains('xt-desktop'); },
    getMode: function () { return mode; },
    getScroll: function () {
      var d = document.scrollingElement || document.documentElement;
      return { top: window.pageYOffset || d.scrollTop || 0, view: window.innerHeight, total: d.scrollHeight };
    }
  };

  // 全局发布菜单：发布商品 / 发晒物（底部"+"与桌面导航"发布"共用）
  window.XtPublish = {
    show: function () {
      var doc = document;
      if (doc.getElementById('xt-publish-menu')) return;
      var mask = doc.createElement('div');
      mask.id = 'xt-publish-menu';
      mask.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,.4);z-index:10000;display:flex;align-items:flex-end;justify-content:center;';
      var sheet = doc.createElement('div');
      sheet.style.cssText = 'width:100%;max-width:480px;background:#fff;border-radius:16px 16px 0 0;padding:10px 0 20px;animation:xt-sheet-up .2s ease;';
      var css = doc.createElement('style');
      css.textContent = '@keyframes xt-sheet-up{from{transform:translateY(40px);opacity:.4}to{transform:none;opacity:1}}';
      doc.head.appendChild(css);
      var items = [
        { label: '发布商品', sub: '上传闲置，等待同城买家', href: '/goods-edit.html', icon: 'el-icon-camera' },
        { label: '发晒物', sub: '记录闲置故事，关联商品', href: '/note-edit.html', icon: 'el-icon-edit' }
      ];
      var close = function () {
        if (mask.parentNode) mask.parentNode.removeChild(mask);
        if (css.parentNode) css.parentNode.removeChild(css);
      };
      mask.addEventListener('click', function (e) { if (e.target === mask) close(); });
      items.forEach(function (it) {
        var row = doc.createElement('div');
        row.style.cssText = 'display:flex;align-items:center;gap:12px;padding:14px 20px;cursor:pointer;border-bottom:1px solid #f2f2f2;';
        row.innerHTML = '<i class="' + (it.icon || 'el-icon-camera') + '" style="font-size:20px;color:#ff6633"></i>' +
          '<div><div style="font-size:15px;font-weight:600">' + it.label + '</div>' +
          '<div style="font-size:12px;color:#999;margin-top:2px">' + it.sub + '</div></div>' +
          '<i class="el-icon-arrow-right" style="margin-left:auto;color:#ccc"></i>';
        row.addEventListener('click', function () { location.href = it.href; });
        sheet.appendChild(row);
      });
      var cancel = doc.createElement('div');
      cancel.style.cssText = 'text-align:center;padding:14px;color:#666;font-size:14px;cursor:pointer;';
      cancel.textContent = '取消';
      cancel.addEventListener('click', close);
      sheet.appendChild(cancel);
      mask.appendChild(sheet);
      doc.body.appendChild(mask);
    }
  };

  function switchMode(m) {
    try { localStorage.setItem(KEY, m); } catch (e) {}
    location.reload();
  }

  // 悬浮切换按钮（两种模式都显示，样式内联注入，不依赖 desktop.css）
  var btnStyle = document.createElement('style');
  btnStyle.textContent =
    '.xt-mode-btn{position:fixed;right:16px;bottom:16px;z-index:99999;display:flex;align-items:center;gap:6px;' +
    'background:rgba(0,0,0,.6);color:#fff;font-size:13px;padding:8px 14px;border-radius:18px;cursor:pointer;' +
    'box-shadow:0 2px 10px rgba(0,0,0,.25);user-select:none;font-family:inherit;line-height:1;border:0;}' +
    '.xt-mode-btn:hover{background:rgba(0,0,0,.75)}' +
    '.xt-mode-btn svg{width:15px;height:15px;fill:#fff;flex:none}';
  document.head.appendChild(btnStyle);

  function makeBtn() {
    var btn = document.createElement('button');
    btn.className = 'xt-mode-btn';
    btn.type = 'button';
    btn.title = mode === 'desktop' ? '切换到手机版' : '切换到电脑版';
    var icon = mode === 'desktop'
      ? '<svg viewBox="0 0 1024 1024"><path d="M720 96H304c-44.1 0-80 35.9-80 80v672c0 44.1 35.9 80 80 80h416c44.1 0 80-35.9 80-80V176c0-44.1-35.9-80-80-80zM512 880c-22.1 0-40-17.9-40-40s17.9-40 40-40 40 17.9 40 40-17.9 40-40 40zM704 768H320V224h384v544z"/></svg>'
      : '<svg viewBox="0 0 1024 1024"><path d="M928 160H96c-17.7 0-32 14.3-32 32v640c0 17.7 14.3 32 32 32h832c17.7 0 32-14.3 32-32V192c0-17.7-14.3-32-32-32zM896 576H640V448h256v128zM576 448h64v128h-64V448zM128 224h768v128H128V224zM128 832V640h448v192H128z"/></svg>';
    btn.innerHTML = icon + (mode === 'desktop' ? '手机版' : '电脑版');
    btn.addEventListener('click', function () {
      switchMode(mode === 'desktop' ? 'mobile' : 'desktop');
    });
    document.body.appendChild(btn);
  }

  // 桌面模式：注入顶部导航栏
  function makeNav() {
    var nav = document.createElement('nav');
    nav.className = 'xt-nav';
    var links = [
      ['/', '首页', 'index'],
      ['/goods-list.html?type=0&name=' + encodeURIComponent('全部'), '商城', 'goods-list'],
      ['/message.html', '消息', 'message'],
      ['/info.html', '我的', 'info']
    ];
    var html = '<div class="xt-nav-inner"><a class="xt-logo" href="/">闲淘</a>';
    for (var i = 0; i < links.length; i++) {
      var cls = 'xt-nav-link' + (links[i][2] === page ? ' active' : '');
      html += '<a class="' + cls + '" href="' + links[i][0] + '">' + links[i][1] + '</a>';
    }
    html += '<div class="xt-nav-right"><a class="xt-nav-post" href="javascript:void(0)" onclick="XtPublish.show()">发布</a></div></div>';
    nav.innerHTML = html;
    document.body.insertBefore(nav, document.body.firstChild);
  }

  if (document.body) {
    if (mode === 'desktop') makeNav();
    makeBtn();
  } else {
    document.addEventListener('DOMContentLoaded', function () {
      if (mode === 'desktop') makeNav();
      makeBtn();
    });
  }
})();
