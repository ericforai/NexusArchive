import{c as s,N as r,j as e,U as n,X as c,Y as l}from"./index-B0ZGCG6f.js";import{S as h}from"./shield-az42GoJG.js";import{L as d}from"./lock-CYzJwHcW.js";/**
 * @license lucide-react v0.554.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const p=[["path",{d:"M10 12h4",key:"a56b0p"}],["path",{d:"M10 8h4",key:"1sr2af"}],["path",{d:"M14 21v-3a2 2 0 0 0-4 0v3",key:"1rgiei"}],["path",{d:"M6 10H4a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-2",key:"secmi2"}],["path",{d:"M6 21V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v16",key:"16ra0t"}]],y=s("building-2",p);/**
 * @license lucide-react v0.554.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const m=[["path",{d:"M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z",key:"1oefj6"}],["path",{d:"M14 2v5a1 1 0 0 0 1 1h5",key:"wfsgrz"}],["circle",{cx:"11.5",cy:"14.5",r:"2.5",key:"1bq0ko"}],["path",{d:"M13.3 16.3 15 18",key:"2quom7"}]],x=s("file-search",m);/**
 * @license lucide-react v0.554.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const k=[["path",{d:"M20 10a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1h-2.5a1 1 0 0 1-.8-.4l-.9-1.2A1 1 0 0 0 15 3h-2a1 1 0 0 0-1 1v5a1 1 0 0 0 1 1Z",key:"hod4my"}],["path",{d:"M20 21a1 1 0 0 0 1-1v-3a1 1 0 0 0-1-1h-2.9a1 1 0 0 1-.88-.55l-.42-.85a1 1 0 0 0-.92-.6H13a1 1 0 0 0-1 1v5a1 1 0 0 0 1 1Z",key:"w4yl2u"}],["path",{d:"M3 5a2 2 0 0 0 2 2h3",key:"f2jnh7"}],["path",{d:"M3 3v13a2 2 0 0 0 2 2h3",key:"k8epm1"}]],b=s("folder-tree",k);/**
 * @license lucide-react v0.554.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const u=[["path",{d:"M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",key:"1yyitq"}],["path",{d:"M16 3.128a4 4 0 0 1 0 7.744",key:"16gr8j"}],["path",{d:"M22 21v-2a4 4 0 0 0-3-3.87",key:"kshegd"}],["circle",{cx:"9",cy:"7",r:"4",key:"nufk8"}]],v=s("users",u),f=[{key:"basic",label:"基础设置",path:"/system/settings/basic",icon:n},{key:"users",label:"用户管理",path:"/system/settings/users",icon:v},{key:"roles",label:"角色权限",path:"/system/settings/roles",icon:h},{key:"org",label:"组织架构",path:"/system/settings/org",icon:y},{key:"fonds",label:"全宗管理",path:"/system/settings/fonds",icon:b},{key:"security",label:"安全合规",path:"/system/settings/security",icon:d},{key:"audit",label:"审计日志",path:"/system/settings/audit",icon:x}],M=()=>{const t=r();return e.jsxs("div",{className:"min-h-full bg-slate-50",children:[e.jsxs("div",{className:"bg-white border-b border-slate-200 px-8 py-6",children:[e.jsx("h1",{className:"text-2xl font-bold text-slate-800",children:"系统设置"}),e.jsx("p",{className:"text-slate-500 mt-1",children:"配置全局参数、用户权限及安全策略"})]}),e.jsx("div",{className:"bg-white border-b border-slate-200 px-8",children:e.jsx("nav",{className:"flex space-x-1 overflow-x-auto","aria-label":"设置导航",children:f.map(a=>{const o=t.pathname===a.path||a.key==="basic"&&t.pathname==="/system/settings",i=a.icon;return e.jsxs(c,{to:a.path,className:`
                  flex items-center px-4 py-3 text-sm font-medium border-b-2 whitespace-nowrap
                  transition-colors duration-200
                  ${o?"border-primary-500 text-primary-600":"border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300"}
                `,children:[e.jsx(i,{size:16,className:"mr-2"}),a.label]},a.key)})})}),e.jsx("div",{className:"p-8 max-w-5xl mx-auto animate-in fade-in slide-in-from-bottom-4 duration-300",children:e.jsx(l,{})})]})};export{M as SettingsLayout,M as default};
