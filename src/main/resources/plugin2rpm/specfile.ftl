%define _prefix %{_usr}/lib/hudson
%define workdir %{_var}/lib/hudson

%define debug_package %{nil}

Name: ${name}
Version: ${version}
Release: 1
Summary: Hudson ${displayName}
URL: ${getURL()}
Group: Development/Tools/Building
License: TODO
BuildRoot: %{_tmppath}/build-%{name}-%{version}

Source: ${artifact.artifactId}.hpi

<#if requiredHudsonVersion!=null>
PreReq: hudson >= ${requiredHudsonVersion}
</#if>
<#if requiredHudsonVersion==null>
PreReq: hudson
</#if>
<#if dependencies != "">
Requires: ${dependencies}
</#if>
BuildArch: noarch

%description
${displayName}

%prep
%setup -q -T -c

%build

%install
%__install -d "%{buildroot}%{workdir}"
%__install -d "%{buildroot}%{workdir}/plugins"

%__install -D -m0644 "%{SOURCE0}" "%{buildroot}%{workdir}/plugins/${artifact.artifactId}.hpi"

%post
/sbin/service hudson restart

%clean
%__rm -rf "%{buildroot}"

%files
%defattr(-,hudson,hudson)
%attr(0755,hudson,hudson) %dir %{workdir}/plugins 
%{workdir}/plugins/${artifact.artifactId}.hpi